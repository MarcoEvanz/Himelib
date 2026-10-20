package com.cuddly.himelib.cache.object;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/**
 * Baked cuboid for a {@link GeoBone}
 * <p>
 * HimeLib: a cube's own turn about its pivot never changes, so it is worked out once, as it is baked - the
 * matrix (pivot, turn, back from the pivot), its normal matrix, and the turn as one quaternion - in place of the
 * three quaternions GeckoLib multiplied into the pose stack for every cube of every model every frame (more than
 * half of a frame's time with a few dozen girls in view). A cube that does not turn ({@link #isRotated}) is
 * drawn with its bone's matrix as it is.
 */
public final class GeoCube {
	private final GeoQuad[] quads;
	private final Vec3 pivot;
	private final Vec3 rotation;
	private final Vec3 size;
	private final double inflate;
	private final boolean mirror;
	private final boolean rotated;
	private final Quaternionf turn;
	private final Matrix4f localPose;
	private final Matrix3f localNormal;

	public GeoCube(GeoQuad[] quads, Vec3 pivot, Vec3 rotation, Vec3 size, double inflate, boolean mirror) {
		this.quads = quads;
		this.pivot = pivot;
		this.rotation = rotation;
		this.size = size;
		this.inflate = inflate;
		this.mirror = mirror;
		this.rotated = rotation.x() != 0 || rotation.y() != 0 || rotation.z() != 0;
		// as RenderUtil.rotateMatrixAroundCube turned it: about Z, then Y, then X
		this.turn = new Quaternionf().rotationZ((float)rotation.z()).rotateY((float)rotation.y()).rotateX((float)rotation.x());
		float px = (float)pivot.x() / 16f, py = (float)pivot.y() / 16f, pz = (float)pivot.z() / 16f;
		this.localPose = new Matrix4f().translation(px, py, pz).rotate(this.turn).translate(-px, -py, -pz);
		this.localNormal = new Matrix3f().rotation(this.turn);
	}

	public GeoQuad[] quads() { return this.quads; }

	public Vec3 pivot() { return this.pivot; }

	public Vec3 rotation() { return this.rotation; }

	public Vec3 size() { return this.size; }

	public double inflate() { return this.inflate; }

	public boolean mirror() { return this.mirror; }

	/** Whether the cube turns about its pivot at all. */
	public boolean isRotated() { return this.rotated; }

	/** Its turn about its pivot, as one quaternion (Z, then Y, then X). */
	public Quaternionfc turn() { return this.turn; }

	/** Its pivot, turn and way back from the pivot, as one matrix (in blocks: the pivot over 16). */
	public Matrix4fc localPose() { return this.localPose; }

	/** Its turn, for the normals. */
	public Matrix3fc localNormal() { return this.localNormal; }
}
