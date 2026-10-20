package com.cuddly.himelib.renderer.batch;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.EntityVertex;
import org.lwjgl.system.MemoryStack;

/**
 * Vertices handed to a buffer through Sodium's vertex writer, many at a time (and through Iris's, which takes
 * Sodium's pushes into its own longer layout). Only loaded when Sodium is.
 */
final class SodiumPush {
	private SodiumPush() {}

	/** At most this many vertices a push: a writer may lay out a copy on the memory stack (64 KB). */
	private static final int CHUNK = 1024;

	/** Pushes {@code count} vertices at {@code ptr} (the entity layout) into {@code consumer}; false when it takes no pushes. */
	static boolean push(VertexConsumer consumer, long ptr, int count) {
		VertexBufferWriter writer = VertexBufferWriter.tryOf(consumer);

		if (writer == null)
			return false;

		for (int done = 0; done < count; done += CHUNK) {
			int n = Math.min(CHUNK, count - done);

			try (MemoryStack stack = MemoryStack.stackPush()) {
				writer.push(stack, ptr + (long)done * CubeBatch.STRIDE, n, EntityVertex.FORMAT);
			}
		}

		return true;
	}
}
