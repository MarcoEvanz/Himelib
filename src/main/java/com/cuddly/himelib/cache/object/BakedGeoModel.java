package com.cuddly.himelib.cache.object;

import com.cuddly.himelib.loading.json.raw.ModelProperties;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Baked model object for Geckolib models
 * <p>
 * HimeLib: a bone found by name from a table made once (the first in tree order, as the search found it), not by a
 * walk of the whole tree per ask - renderers ask for a few bones by name every frame, and a girl has some 400.
 */
public final class BakedGeoModel {
	private final List<GeoBone> topLevelBones;
	private final ModelProperties properties;
	private volatile Map<String, GeoBone> byName;

	public BakedGeoModel(List<GeoBone> topLevelBones, ModelProperties properties) {
		this.topLevelBones = topLevelBones;
		this.properties = properties;
	}

	public List<GeoBone> topLevelBones() {
		return this.topLevelBones;
	}

	public ModelProperties properties() {
		return this.properties;
	}

	/**
	 * Gets a bone from this model by name
	 *
	 * @param name The name of the bone
	 * @return An {@link Optional} containing the {@link GeoBone} if one matches, otherwise an empty Optional
	 */
	public Optional<GeoBone> getBone(String name) {
		Map<String, GeoBone> bones = this.byName;

		if (bones == null) {
			bones = new Object2ObjectOpenHashMap<>();

			for (GeoBone bone : this.topLevelBones) {
				index(bone, bones);
			}

			this.byName = bones;
		}

		return Optional.ofNullable(bones.get(name));
	}

	private static void index(GeoBone bone, Map<String, GeoBone> bones) {
		bones.putIfAbsent(bone.getName(), bone);

		for (GeoBone child : bone.getChildBones()) {
			index(child, bones);
		}
	}

	/**
	 * Search a given {@link GeoBone}'s child bones and see if any of them match the given name, then return it
	 *
	 * @param parent The parent bone to search the children of
	 * @param name The name of the child bone to find
	 * @return The {@code GeoBone} found in the parent's children list, or null if not found
	 */
	public GeoBone searchForChildBone(GeoBone parent, String name) {
		if (parent.getName().equals(name))
			return parent;

		for (GeoBone bone : parent.getChildBones()) {
			if (bone.getName().equals(name))
				return bone;

			GeoBone subChildBone = searchForChildBone(bone, name);

			if (subChildBone != null)
				return subChildBone;
		}

		return null;
	}
}
