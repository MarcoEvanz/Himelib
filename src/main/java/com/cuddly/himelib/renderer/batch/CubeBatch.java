package com.cuddly.himelib.renderer.batch;

import com.cuddly.himelib.cache.object.GeoBone;
import com.cuddly.himelib.cache.object.GeoCube;
import com.cuddly.himelib.cache.object.GeoQuad;
import com.cuddly.himelib.cache.object.GeoVertex;
import com.cuddly.himelib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HimeLib: a model's cubes drawn many threads at once. As a renderer that asks for it walks the bones (on the
 * render thread, as ever: the animation and every bone's matrix are worked out there), each bone's cubes are only
 * noted with the bone's matrix; then {@link #flush} has the vertices worked out and written side by side on the
 * mesh threads (and the render thread), straight into memory in the entity vertex layout, and hands them to the
 * buffers in the order they were noted - with Sodium, in one push per buffer (as Touhou Little Maid's Sodium writer
 * does, and which Iris takes too); without it, vertex by vertex, the arithmetic already done.
 * <p>
 * A batch is flushed before anything else is asked of the buffers (another render type: a held item, a bone drawn
 * with another texture), so what is drawn, and in what order, is what the renderer would have drawn itself.
 */
public final class CubeBatch {
	/** The entity vertex layout (DefaultVertexFormat.NEW_ENTITY): position, colour, uv, overlay, light, normal. */
	static final int STRIDE = 36;
	/** Fewer vertices than this are worked out on the render thread alone (handing them out costs more). */
	static final int PARALLEL_FROM = 6000;

	private static final boolean SODIUM = FabricLoader.getInstance().isModLoaded("sodium");
	private static final int THREADS = Math.max(1, Math.min(6, Runtime.getRuntime().availableProcessors() - 2));
	private static final ExecutorService MESH = Executors.newFixedThreadPool(THREADS, new java.util.concurrent.ThreadFactory() {
		private final AtomicInteger n = new AtomicInteger();

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "HimeLib-Mesh-" + n.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});

	/** One bone's cubes, noted: its matrices at the time, where its vertices go, and how many. */
	private record Entry(GeoBone bone, Matrix4f pose, Matrix3f normal, VertexConsumer consumer,
						 int light, int overlay, int colourABGR, int fixedNormal, int first, int count) {}

	/** No fixed normal: each quad's own. */
	private static final int OWN_NORMAL = Integer.MIN_VALUE;

	/** For measuring (render thread): flushes, the ones handed out to the mesh threads, vertices, since the last ask. */
	private static long statFlushes, statParallel, statVertices;

	/** Flushes, parallel flushes and vertices since the last call (and the counts started again). Render thread. */
	public static long[] takeStats() {
		long[] stats = {statFlushes, statParallel, statVertices};
		statFlushes = statParallel = statVertices = 0;
		return stats;
	}

	private final List<Entry> entries = new ArrayList<>();
	private int vertices;
	private boolean active;
	@Nullable private RenderType layer;
	private long memory;
	private long capacity;

	/** Starts noting cubes; false when this batch is already noting (a model drawn inside another), which then draws its own. */
	public boolean begin(@Nullable RenderType layer) {
		if (this.active)
			return false;

		this.active = true;
		this.layer = layer;
		return true;
	}

	public boolean isActive() {
		return this.active;
	}

	/** The rest drawn, and the batch closed. */
	public void end() {
		try {
			flush();
		}
		finally {
			this.active = false;
			this.layer = null;
		}
	}

	/** {@code bufferSource}, flushing this batch before a buffer of another render type is taken from it. */
	public MultiBufferSource wrap(MultiBufferSource bufferSource) {
		return renderType -> {
			if (renderType != this.layer) {
				flush();
				this.layer = renderType;
			}

			return bufferSource.getBuffer(renderType);
		};
	}

	/** Notes {@code bone}'s cubes, to be drawn into {@code consumer} with the pose stack's matrices as they are now. */
	public void add(GeoBone bone, PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int colour) {
		int count = bone.getCubeVertexCount();

		if (count == 0)
			return;

		// a consumer that only fixes the normal: that normal written here, the vertices handed to the buffer behind it
		int fixedNormal = OWN_NORMAL;

		while (consumer instanceof BatchableConsumer batchable) {
			fixedNormal = batchable.batchNormal();
			consumer = batchable.batchTarget();
		}

		this.entries.add(new Entry(bone, new Matrix4f(pose.pose()), new Matrix3f(pose.normal()), consumer, light, overlay,
				argbToAbgr(colour), fixedNormal, this.vertices, count));
		this.vertices += count;
	}

	/** Every cube noted so far worked out, written and handed to its buffer. Render thread. */
	public void flush() {
		if (this.entries.isEmpty())
			return;

		try {
			statFlushes++;
			statVertices += this.vertices;
			ensureCapacity((long)this.vertices * STRIDE);
			fill();
			hand();
		}
		finally {
			this.entries.clear();
			this.vertices = 0;
		}
	}

	private void fill() {
		int n = this.entries.size();

		if (this.vertices < PARALLEL_FROM || THREADS < 1) {
			write(0, n);
			return;
		}

		statParallel++;
		// cut into small runs of about equal vertex counts, taken one after another by whichever thread is free -
		// the mesh threads and the render thread alike - so none waits on another's longer share
		int target = Math.max(1024, this.vertices / ((THREADS + 1) * 4));
		List<int[]> runs = new ArrayList<>();
		int start = 0;
		int acc = 0;

		for (int i = 0; i < n; i++) {
			acc += this.entries.get(i).count();

			if (acc >= target) {
				runs.add(new int[] {start, i + 1});
				start = i + 1;
				acc = 0;
			}
		}

		if (start < n)
			runs.add(new int[] {start, n});

		AtomicInteger next = new AtomicInteger();
		int helpers = Math.min(THREADS, runs.size() - 1);
		CountDownLatch done = new CountDownLatch(helpers);
		Throwable[] failed = new Throwable[1];
		Runnable work = () -> {
			int r;

			while ((r = next.getAndIncrement()) < runs.size()) {
				write(runs.get(r)[0], runs.get(r)[1]);
			}
		};

		for (int h = 0; h < helpers; h++) {
			MESH.execute(() -> {
				try {
					work.run();
				}
				catch (Throwable t) {
					failed[0] = t;
					next.set(Integer.MAX_VALUE / 2);
				}
				finally {
					done.countDown();
				}
			});
		}

		work.run();

		try {
			done.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (failed[0] != null)
			throw new IllegalStateException("A mesh thread failed", failed[0]);
	}

	/** Entries {@code from} (inclusive) to {@code to}: their vertices worked out and written. Any thread. */
	private void write(int from, int to) {
		Matrix4f cubePose = new Matrix4f();
		Matrix3f cubeNormal = new Matrix3f();
		Matrix3f smoothState = new Matrix3f();
		Vector4f position = new Vector4f();
		Vector3f normal = new Vector3f();
		Vector3f smooth = new Vector3f();

		for (int e = from; e < to; e++) {
			Entry entry = this.entries.get(e);
			boolean ownNormal = entry.fixedNormal() == OWN_NORMAL;
			long ptr = this.memory + (long)entry.first() * STRIDE;
			long end = ptr + (long)entry.count() * STRIDE;

			for (GeoCube cube : entry.bone().getCubes()) {
				Matrix4f pose = entry.pose();
				Matrix3f normalState = entry.normal();

				if (cube.isRotated()) {
					pose = cubePose.set(entry.pose()).mul(cube.localPose());
					normalState = cubeNormal.set(entry.normal()).mul(cube.localNormal());
				}

				for (GeoQuad quad : cube.quads()) {
					if (quad == null)
						continue;

					int packedNormal = entry.fixedNormal();
					Vector3f[] vertexNormals = ownNormal ? quad.vertexNormals() : null;

					if (ownNormal) {
						normalState.transform(quad.normal(), normal);
						RenderUtil.fixInvertedFlatCube(cube, normal);
						packedNormal = packNormal(normal.x, normal.y, normal.z);
					}

					if (vertexNormals != null)
						smoothState.set(pose);

					GeoVertex[] quadVertices = quad.vertices();

					for (int i = 0; i < quadVertices.length; i++) {
						GeoVertex vertex = quadVertices[i];
						Vector3f p = vertex.position();
						pose.transform(p.x(), p.y(), p.z(), 1f, position);
						int n = packedNormal;

						if (vertexNormals != null && i < vertexNormals.length && vertexNormals[i] != null) {
							smoothState.transform(vertexNormals[i], smooth).normalize();
							n = packNormal(smooth.x, smooth.y, smooth.z);
						}

						if (ptr >= end)
							throw new IllegalStateException("bone " + entry.bone().getName() + " has more vertices than it counted");

						MemoryUtil.memPutFloat(ptr, position.x);
						MemoryUtil.memPutFloat(ptr + 4, position.y);
						MemoryUtil.memPutFloat(ptr + 8, position.z);
						MemoryUtil.memPutInt(ptr + 12, entry.colourABGR());
						MemoryUtil.memPutFloat(ptr + 16, vertex.texU());
						MemoryUtil.memPutFloat(ptr + 20, vertex.texV());
						MemoryUtil.memPutInt(ptr + 24, entry.overlay());
						MemoryUtil.memPutInt(ptr + 28, entry.light());
						MemoryUtil.memPutInt(ptr + 32, n);
						ptr += STRIDE;
					}
				}
			}
		}
	}

	/** The written vertices to their buffers, in the order noted: a run into one buffer at a time. Render thread. */
	private void hand() {
		int n = this.entries.size();
		int i = 0;

		while (i < n) {
			VertexConsumer consumer = this.entries.get(i).consumer();
			int first = this.entries.get(i).first();
			int count = 0;

			while (i < n && this.entries.get(i).consumer() == consumer) {
				count += this.entries.get(i).count();
				i++;
			}

			long ptr = this.memory + (long)first * STRIDE;

			if (SODIUM && SodiumPush.push(consumer, ptr, count))
				continue;

			if (consumer instanceof com.mojang.blaze3d.vertex.BufferBuilder builder && VanillaPush.push(builder, ptr, count))
				continue;

			replay(consumer, ptr, count);
		}
	}

	/** Vertex by vertex, into a buffer Sodium cannot take a push into (or with no Sodium). */
	private static void replay(VertexConsumer consumer, long ptr, int count) {
		for (int v = 0; v < count; v++, ptr += STRIDE) {
			int abgr = MemoryUtil.memGetInt(ptr + 12);
			int normal = MemoryUtil.memGetInt(ptr + 32);

			consumer.addVertex(MemoryUtil.memGetFloat(ptr), MemoryUtil.memGetFloat(ptr + 4), MemoryUtil.memGetFloat(ptr + 8),
					abgrToArgb(abgr), MemoryUtil.memGetFloat(ptr + 16), MemoryUtil.memGetFloat(ptr + 20),
					MemoryUtil.memGetInt(ptr + 24), MemoryUtil.memGetInt(ptr + 28),
					(byte)normal / 127f, (byte)(normal >> 8) / 127f, (byte)(normal >> 16) / 127f);
		}
	}

	private void ensureCapacity(long bytes) {
		if (bytes <= this.capacity)
			return;

		long size = Math.max(bytes, this.capacity * 3 / 2);
		this.memory = this.memory == 0 ? MemoryUtil.nmemAlloc(size) : MemoryUtil.nmemRealloc(this.memory, size);

		if (this.memory == 0)
			throw new OutOfMemoryError("No memory for a model's vertices (" + size + " bytes)");

		this.capacity = size;
	}

	/** As the vanilla buffer packs a normal: each axis held to -1..1, to a signed byte. */
	public static int packNormal(float x, float y, float z) {
		return normalByte(x) | (normalByte(y) << 8) | (normalByte(z) << 16);
	}

	private static int normalByte(float f) {
		return (int)(Math.max(-1f, Math.min(1f, f)) * 127f) & 0xFF;
	}

	static int argbToAbgr(int argb) {
		return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
	}

	static int abgrToArgb(int abgr) {
		return (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
	}
}
