package com.cuddly.himelib.loading;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import com.cuddly.himelib.cache.object.BakedGeoModel;
import com.cuddly.himelib.loading.json.raw.Model;
import com.cuddly.himelib.loading.json.typeadapter.KeyFramesAdapter;
import com.cuddly.himelib.loading.object.BakedAnimations;
import com.cuddly.himelib.loading.object.BakedModelFactory;
import com.cuddly.himelib.loading.object.GeometryTree;

/**
 * HimeLib: bakes models and animations from JSON held in memory, the way the resource reload bakes the
 * resource packs' files - for a mod that brings its own (from its own packs, from the network) and puts them
 * in with {@link com.cuddly.himelib.cache.GeckoLibCache#setRuntimeAssets}. Safe off the main thread.
 */
public final class RuntimeLoader {
	private RuntimeLoader() {}

	/** A {@code .geo.json}, baked as the model at {@code location} (its namespace picks the model factory). */
	public static BakedGeoModel model(ResourceLocation location, String json) {
		Model model = KeyFramesAdapter.GEO_GSON.fromJson(JsonParser.parseString(json), Model.class);

		return BakedModelFactory.getForNamespace(location.getNamespace()).constructGeoModel(GeometryTree.fromModel(model));
	}

	/** An {@code .animation.json}, baked. */
	public static BakedAnimations animations(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();

		return KeyFramesAdapter.GEO_GSON.fromJson(GsonHelper.getAsJsonObject(root, "animations"), BakedAnimations.class);
	}
}
