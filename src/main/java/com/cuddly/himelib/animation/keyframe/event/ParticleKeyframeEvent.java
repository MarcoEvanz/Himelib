package com.cuddly.himelib.animation.keyframe.event;

import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animation.AnimationController;
import com.cuddly.himelib.animation.keyframe.event.data.ParticleKeyframeData;

/**
 * The {@link KeyFrameEvent} specific to the {@link AnimationController#particleKeyframeHandler}
 * <p>
 * Called when a particle instruction keyframe is encountered
 */
public class ParticleKeyframeEvent<T extends GeoAnimatable> extends KeyFrameEvent<T, ParticleKeyframeData> {
	public ParticleKeyframeEvent(T animatable, double animationTick, AnimationController<T> controller, ParticleKeyframeData particleKeyFrameData) {
		super(animatable, animationTick, controller, particleKeyFrameData);
	}

	/**
	 * Get the {@link ParticleKeyframeData} relevant to this event call
	 */
	@Override
	public ParticleKeyframeData getKeyframeData() {
		return super.getKeyframeData();
	}
}
