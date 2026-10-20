package com.cuddly.himelib.renderer.batch;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.lwjgl.system.MemoryUtil;

/**
 * Finished entity vertices copied into a vanilla buffer in one go (no Sodium to push them): what its own fast path
 * does vertex by vertex - the last vertex closed, room taken, the vertex written - done once for the run.
 */
final class VanillaPush {
	private VanillaPush() {}

	/** Copies {@code count} vertices at {@code ptr} into {@code builder}; false when it is not an entity buffer it can take them as they are. */
	static boolean push(BufferBuilder builder, long ptr, int count) {
		if (!builder.building || !builder.fastFormat || builder.format != DefaultVertexFormat.NEW_ENTITY || builder.vertexSize != CubeBatch.STRIDE)
			return false;

		if (count == 0)
			return true;

		builder.ensureBuilding();
		builder.endLastVertex();

		long bytes = (long)count * CubeBatch.STRIDE;

		if (bytes > Integer.MAX_VALUE)
			return false;

		long dst = builder.buffer.reserve((int)bytes);
		MemoryUtil.memCopy(ptr, dst, bytes);
		builder.vertices += count;
		builder.vertexPointer = dst + bytes - CubeBatch.STRIDE;
		return true;
	}
}
