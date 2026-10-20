/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package com.cuddly.himelib.animation.keyframe;

import com.cuddly.himelib.animation.AnimationController;

import java.io.Serial;
import java.util.LinkedList;

/**
 * An {@link AnimationPoint} queue holds a queue of {@code AnimationPoints} which are used in
 * the {@link AnimationController} to lerp between values
 */
public final class AnimationPointQueue extends LinkedList<AnimationPoint> {
	@Serial
	private static final long serialVersionUID = 5472797438476621193L;
}
