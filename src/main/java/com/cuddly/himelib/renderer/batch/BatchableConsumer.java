package com.cuddly.himelib.renderer.batch;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A vertex consumer that only puts one normal on every vertex before handing it on (a model drawn unlit, say): the
 * {@link CubeBatch} writes that normal itself and hands the vertices straight to the buffer behind it, many at a
 * time, rather than through it one by one.
 */
public interface BatchableConsumer {
	/** The buffer the vertices go on to. */
	VertexConsumer batchTarget();

	/** The normal every vertex is given, packed as the vanilla buffer packs one ({@link CubeBatch#packNormal}). */
	int batchNormal();
}
