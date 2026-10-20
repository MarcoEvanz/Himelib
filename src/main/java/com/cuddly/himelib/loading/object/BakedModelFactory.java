package com.cuddly.himelib.loading.object;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.cache.object.*;
import com.cuddly.himelib.loading.json.raw.*;
import com.cuddly.himelib.util.GeckoLibUtil;
import com.cuddly.himelib.util.RenderUtil;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Base interface for a factory of {@link BakedGeoModel} objects
 * <p>
 * Handled by default by GeckoLib, but custom implementations may be added by other mods for special needs
 */
public interface BakedModelFactory {
	Map<String, BakedModelFactory> FACTORIES = new Object2ObjectOpenHashMap<>(1);
	BakedModelFactory DEFAULT_FACTORY = new Builtin();

	/**
	 * Construct the output model from the given {@link GeometryTree}.<br>
	 */
	BakedGeoModel constructGeoModel(GeometryTree geometryTree);

	/**
	 * Construct a {@link GeoBone} from the relevant raw input data
	 *
	 * @param boneStructure The {@code BoneStructure} comprising the structure of the bone and its children
	 * @param properties The loaded properties for the model
	 * @param parent The parent bone for this bone, or null if a top-level bone
	 */
	GeoBone constructBone(BoneStructure boneStructure, ModelProperties properties, @Nullable GeoBone parent);

	/**
	 * Construct a {@link GeoCube} from the relevant raw input data
	 *
	 * @param cube The raw {@code Cube} comprising the structure and properties of the cube
	 * @param properties The loaded properties for the model
	 * @param bone The bone this cube belongs to
	 */
	GeoCube constructCube(Cube cube, ModelProperties properties, GeoBone bone);

	/**
	 * Builtin method to construct the quad list from the various vertices and related data, to make it easier
	 * <p>
	 * Vertices have already been mirrored here if {@code mirror} is true
	 */
	default GeoQuad[] buildQuads(UVUnion uvUnion, VertexSet vertices, Cube cube, float textureWidth, float textureHeight, boolean mirror) {
		GeoQuad[] quads = new GeoQuad[6];

		quads[0] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.WEST);
		quads[1] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.EAST);
		quads[2] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.NORTH);
		quads[3] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.SOUTH);
		quads[4] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.UP);
		quads[5] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, Direction.DOWN);

		return quads;
	}

	/**
	 * HimeLib: a bone's {@code poly_mesh}, baked as one more cube of its bone - no pivot, no turn of its own - whose
	 * quads are the mesh's polygons (a triangle as a quad with its last corner twice). Drawn, animated, hidden and
	 * deformed exactly as the bone's cubes are; neighbouring polygons share their corners' positions, so whatever moves
	 * a point moves them alike and the surface never parts.
	 * <p>
	 * Positions are in the model's pixels, as cube origins (x mirrored and divided by 16, as cubes are baked), so the
	 * polygons' order is reversed to keep them facing out. UVs are 0..1 with v from the bottom when
	 * {@code normalized_uvs} (as Blockbench writes them), else texture pixels from the top left. The quad's normal is
	 * its corners' normals averaged (or its own face's, when the mesh gives none): flat shaded, as cubes are.
	 */
	@Nullable
	default GeoCube constructMesh(PolyMesh mesh, ModelProperties properties) {
		double[] positions = mesh.positions();
		double[] normals = mesh.normals();
		double[] uvs = mesh.uvs();
		PolysUnion polys = mesh.polysUnion();

		if (polys == null || positions.length < 9)
			return null;

		boolean normalized = mesh.normalizedUVs() == Boolean.TRUE;
		float textureWidth = (float)properties.textureWidth();
		float textureHeight = (float)properties.textureHeight();
		List<int[][]> polygons = new ObjectArrayList<>();

		if (polys.type() != null) {
			// tri_list / quad_list: the listed vertices in order, each its own position, normal and uv
			int corners = polys.type() == PolysUnion.Type.TRI ? 3 : 4;
			int count = positions.length / 3;

			for (int i = 0; i + corners <= count; i += corners) {
				int[][] polygon = new int[corners][];

				for (int c = 0; c < corners; c++) {
					polygon[c] = new int[] {i + c, i + c, i + c};
				}

				polygons.add(polygon);
			}
		}
		else {
			for (double[][] poly : polys.union()) {
				int[][] polygon = new int[poly.length][];

				for (int c = 0; c < poly.length; c++) {
					polygon[c] = new int[] {(int)poly[c][0], poly[c].length > 1 ? (int)poly[c][1] : -1, poly[c].length > 2 ? (int)poly[c][2] : -1};
				}

				polygons.add(polygon);
			}
		}

		List<GeoQuad> quads = new ObjectArrayList<>();

		for (int[][] polygon : polygons) {
			if (polygon.length < 3 || polygon.length > 4)
				continue;

			GeoVertex[] vertices = new GeoVertex[4];
			org.joml.Vector3f[] vertexNormals = new org.joml.Vector3f[4];
			float nx = 0, ny = 0, nz = 0;
			boolean hasNormals = true;

			for (int c = 0; c < polygon.length; c++) {
				// reversed: mirroring x turns the polygon over
				int[] corner = polygon[polygon.length - 1 - c];
				int p = corner[0] * 3;

				if (p < 0 || p + 2 >= positions.length)
					return null;

				float u = 0, v = 0;
				int t = corner[2] * 2;

				if (corner[2] >= 0 && t + 1 < uvs.length) {
					u = normalized ? (float)uvs[t] : (float)uvs[t] / textureWidth;
					v = normalized ? 1 - (float)uvs[t + 1] : (float)uvs[t + 1] / textureHeight;
				}

				vertices[c] = new GeoVertex(new org.joml.Vector3f((float)(-positions[p] / 16d), (float)(positions[p + 1] / 16d), (float)(positions[p + 2] / 16d)), u, v);
				int n = corner[1] * 3;

				if (corner[1] >= 0 && n + 2 < normals.length) {
					nx -= (float)normals[n];
					ny += (float)normals[n + 1];
					nz += (float)normals[n + 2];
					vertexNormals[c] = new org.joml.Vector3f(-(float)normals[n], (float)normals[n + 1], (float)normals[n + 2]);
				}
				else {
					hasNormals = false;
				}
			}

			if (polygon.length == 3) {
				vertices[3] = vertices[2];
				vertexNormals[3] = vertexNormals[2];
			}

			org.joml.Vector3f normal = new org.joml.Vector3f(nx, ny, nz);

			if (!hasNormals || normal.lengthSquared() < 1e-12f) {
				org.joml.Vector3f a = new org.joml.Vector3f(vertices[1].position()).sub(vertices[0].position());
				org.joml.Vector3f b = new org.joml.Vector3f(vertices[2].position()).sub(vertices[0].position());

				normal = a.cross(b);
			}

			if (normal.lengthSquared() < 1e-12f)
				normal.set(0, 1, 0);

			normal.normalize();
			quads.add(new GeoQuad(vertices, normal, Direction.getNearest(normal.x, normal.y, normal.z),
					hasNormals ? smoothNormals(vertexNormals) : null));
		}

		if (quads.isEmpty())
			return null;

		return new GeoCube(quads.toArray(new GeoQuad[0]), Vec3.ZERO, Vec3.ZERO, new Vec3(1, 1, 1), 0, false);
	}

	/**
	 * HimeLib: a mesh polygon's corner normals, normalized, when they differ (a smooth surface, lit vertex by vertex);
	 * null when they are all one (a flat face: its quad normal does).
	 */
	@Nullable
	static org.joml.Vector3f[] smoothNormals(org.joml.Vector3f[] normals) {
		boolean differ = false;

		for (org.joml.Vector3f n : normals) {
			if (n == null || n.lengthSquared() < 1e-12f)
				return null;

			n.normalize();
			differ |= n.distanceSquared(normals[0]) > 1e-8f;
		}

		return differ ? normals : null;
	}

	/**
	 * Build an individual quad
	 */
	default GeoQuad buildQuad(VertexSet vertices, Cube cube, UVUnion uvUnion, float textureWidth, float textureHeight, boolean mirror, Direction direction) {
		if (!uvUnion.isBoxUV()) {
			FaceUV faceUV = uvUnion.faceUV().fromDirection(direction);

			if (faceUV == null)
				return null;

			return GeoQuad.build(vertices.verticesForQuad(direction, false, mirror || cube.mirror() == Boolean.TRUE), faceUV.uv(), faceUV.uvSize(),
					faceUV.uvRotation(), textureWidth, textureHeight, mirror, direction);
		}

		double[] uv = cube.uv().boxUVCoords();
		double[] uvSize = cube.size();
		Vec3 uvSizeVec = new Vec3(Math.floor(uvSize[0]), Math.floor(uvSize[1]), Math.floor(uvSize[2]));
		double[][] uvData = switch(direction) {
			case WEST -> new double[][] {
					new double[] {uv[0] + uvSizeVec.z + uvSizeVec.x, uv[1] + uvSizeVec.z},
					new double[] {uvSizeVec.z, uvSizeVec.y}
			};
			case EAST -> new double[][] {
					new double[] { uv[0], uv[1] + uvSizeVec.z },
					new double[] { uvSizeVec.z, uvSizeVec.y }
			};
			case NORTH -> new double[][] {
					new double[] {uv[0] + uvSizeVec.z, uv[1] + uvSizeVec.z},
					new double[] {uvSizeVec.x, uvSizeVec.y}
			};
			case SOUTH -> new double[][] {
					new double[] {uv[0] + uvSizeVec.z + uvSizeVec.x + uvSizeVec.z, uv[1] + uvSizeVec.z},
					new double[] {uvSizeVec.x, uvSizeVec.y }
			};
			case UP -> new double[][] {
					new double[] {uv[0] + uvSizeVec.z, uv[1]},
					new double[] {uvSizeVec.x, uvSizeVec.z}
			};
			case DOWN -> new double[][] {
					new double[] {uv[0] + uvSizeVec.z + uvSizeVec.x, uv[1] + uvSizeVec.z},
					new double[] {uvSizeVec.x, -uvSizeVec.z}
			};
		};

		return GeoQuad.build(vertices.verticesForQuad(direction, true, mirror || cube.mirror() == Boolean.TRUE), uvData[0], uvData[1], FaceUV.Rotation.NONE, textureWidth, textureHeight, mirror, direction);
	}

	static BakedModelFactory getForNamespace(String namespace) {
		return FACTORIES.getOrDefault(namespace, DEFAULT_FACTORY);
	}

	/**
	 * Register a custom {@link BakedModelFactory} to handle loading models in a custom way
	 * <p>
	 * <b><u>MUST be called during mod construct</u></b>
	 * <p>
	 * It is recommended you don't call this directly, and instead call it via {@link GeckoLibUtil#addCustomBakedModelFactory}
	 *
	 * @param namespace The namespace (modid) to register the factory for
	 * @param factory The factory responsible for model loading under the given namespace
	 */
	static void register(String namespace, BakedModelFactory factory) {
		FACTORIES.put(namespace, factory);
	}

	final class Builtin implements BakedModelFactory {
		@Override
		public BakedGeoModel constructGeoModel(GeometryTree geometryTree) {
			List<GeoBone> bones = new ObjectArrayList<>();

			for (BoneStructure boneStructure : geometryTree.topLevelBones().values()) {
				bones.add(constructBone(boneStructure, geometryTree.properties(), null));
			}

			return new BakedGeoModel(bones, geometryTree.properties());
		}

		@Override
		public GeoBone constructBone(BoneStructure boneStructure, ModelProperties properties, GeoBone parent) {
			Bone bone = boneStructure.self();
			GeoBone newBone = new GeoBone(parent, bone.name(), bone.mirror(), bone.inflate(), bone.neverRender(), bone.reset());
			Vec3 rotation = RenderUtil.arrayToVec(bone.rotation());
			Vec3 pivot = RenderUtil.arrayToVec(bone.pivot());

			newBone.updateRotation((float)Math.toRadians(-rotation.x), (float)Math.toRadians(-rotation.y), (float)Math.toRadians(rotation.z));
			newBone.updatePivot((float)-pivot.x, (float)pivot.y, (float)pivot.z);

			for (Cube cube : bone.cubes()) {
				newBone.getCubes().add(constructCube(cube, properties, newBone));
			}

			if (bone.polyMesh() != null) {
				GeoCube mesh = constructMesh(bone.polyMesh(), properties);

				if (mesh != null)
					newBone.getCubes().add(mesh);
			}

			for (BoneStructure child : boneStructure.children().values()) {
				newBone.getChildBones().add(constructBone(child, properties, newBone));
			}

			return newBone;
		}

		@Override
		public GeoCube constructCube(Cube cube, ModelProperties properties, GeoBone bone) {
			boolean mirror = cube.mirror() == Boolean.TRUE;
			double inflate = cube.inflate() != null ? cube.inflate() / 16f : (bone.getInflate() == null ? 0 : bone.getInflate() / 16f);
			Vec3 size = RenderUtil.arrayToVec(cube.size());
			Vec3 origin = RenderUtil.arrayToVec(cube.origin());
			Vec3 rotation = RenderUtil.arrayToVec(cube.rotation());
			Vec3 pivot = RenderUtil.arrayToVec(cube.pivot());
			origin = new Vec3(-(origin.x + size.x) / 16d, origin.y / 16d, origin.z / 16d);
			Vec3 vertexSize = size.multiply(1 / 16d, 1 / 16d, 1 / 16d);

			pivot = pivot.multiply(-1, 1, 1);
			rotation = new Vec3(Math.toRadians(-rotation.x), Math.toRadians(-rotation.y), Math.toRadians(rotation.z));
			GeoQuad[] quads = buildQuads(cube.uv(), new VertexSet(origin, vertexSize, inflate), cube, (float)properties.textureWidth(), (float)properties.textureHeight(), mirror);

			return new GeoCube(quads, pivot, rotation, size, inflate, mirror);
		}
	}

	/**
	 * Holder class to make it easier to store and refer to vertices for a given cube
	 */
	record VertexSet(GeoVertex bottomLeftBack, GeoVertex bottomRightBack, GeoVertex topLeftBack, GeoVertex topRightBack,
							GeoVertex topLeftFront, GeoVertex topRightFront, GeoVertex bottomLeftFront, GeoVertex bottomRightFront) {
		public VertexSet(Vec3 origin, Vec3 vertexSize, double inflation) {
			this(
					new GeoVertex(origin.x - inflation, origin.y - inflation, origin.z - inflation),
					new GeoVertex(origin.x - inflation, origin.y - inflation, origin.z + vertexSize.z + inflation),
					new GeoVertex(origin.x - inflation, origin.y + vertexSize.y + inflation, origin.z - inflation),
					new GeoVertex(origin.x - inflation, origin.y + vertexSize.y + inflation, origin.z + vertexSize.z + inflation),
					new GeoVertex(origin.x + vertexSize.x + inflation, origin.y + vertexSize.y + inflation, origin.z - inflation),
					new GeoVertex(origin.x + vertexSize.x + inflation, origin.y + vertexSize.y + inflation, origin.z + vertexSize.z + inflation),
					new GeoVertex(origin.x + vertexSize.x + inflation, origin.y - inflation, origin.z - inflation),
					new GeoVertex(origin.x + vertexSize.x + inflation, origin.y - inflation, origin.z + vertexSize.z + inflation));
		}

		/**
		 * Returns the normal vertex array for a west-facing quad
		 */
		public GeoVertex[] quadWest() {
			return new GeoVertex[] {this.topRightBack, this.topLeftBack, this.bottomLeftBack, this.bottomRightBack};
		}

		/**
		 * Returns the normal vertex array for an east-facing quad
		 */
		public GeoVertex[] quadEast() {
			return new GeoVertex[] {this.topLeftFront, this.topRightFront, this.bottomRightFront, this.bottomLeftFront};
		}

		/**
		 * Returns the normal vertex array for a north-facing quad
		 */
		public GeoVertex[] quadNorth() {
			return new GeoVertex[] {this.topLeftBack, this.topLeftFront, this.bottomLeftFront, this.bottomLeftBack};
		}

		/**
		 * Returns the normal vertex array for a south-facing quad
		 */
		public GeoVertex[] quadSouth() {
			return new GeoVertex[] {this.topRightFront, this.topRightBack, this.bottomRightBack, this.bottomRightFront};
		}

		/**
		 * Returns the normal vertex array for a top-facing quad
		 */
		public GeoVertex[] quadUp() {
			return new GeoVertex[] {this.topRightBack, this.topRightFront, this.topLeftFront, this.topLeftBack};
		}

		/**
		 * Returns the normal vertex array for a bottom-facing quad
		 */
		public GeoVertex[] quadDown() {
			return new GeoVertex[] {this.bottomLeftBack, this.bottomLeftFront, this.bottomRightFront, this.bottomRightBack};
		}

		/**
		 * Return the vertex array relevant to the quad being built, taking into account mirroring and quad type
		 */
		public GeoVertex[] verticesForQuad(Direction direction, boolean boxUv, boolean mirror) {
			return switch (direction) {
				case WEST -> mirror ? quadEast() : quadWest();
				case EAST -> mirror ? quadWest() : quadEast();
				case NORTH -> quadNorth();
				case SOUTH -> quadSouth();
				case UP -> mirror && !boxUv ? quadDown() : quadUp();
				case DOWN -> mirror && !boxUv ? quadUp() : quadDown();
			};
		}
	}
}
