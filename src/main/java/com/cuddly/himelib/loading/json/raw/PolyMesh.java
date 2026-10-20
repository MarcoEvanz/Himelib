package com.cuddly.himelib.loading.json.raw;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.util.JsonUtil;

/**
 * Container class for poly mesh information, only used in deserialization at startup
 */
public record PolyMesh(@Nullable Boolean normalizedUVs, double[] normals, @Nullable PolysUnion polysUnion, double[] positions, double[] uvs) {
	public static JsonDeserializer<PolyMesh> deserializer() throws JsonParseException {
		return (json, type, context) -> {
			JsonObject obj = json.getAsJsonObject();
			Boolean normalizedUVs = JsonUtil.getOptionalBoolean(obj, "normalized_uvs");
			double[] normals = flatten(GsonHelper.getAsJsonArray(obj, "normals", null));
			PolysUnion polysUnion = GsonHelper.getAsObject(obj, "polys", null, context, PolysUnion.class);
			double[] positions = flatten(GsonHelper.getAsJsonArray(obj, "positions", null));
			double[] uvs = flatten(GsonHelper.getAsJsonArray(obj, "uvs", null));

			return new PolyMesh(normalizedUVs, normals, polysUnion, positions, uvs);
		};
	}

	/**
	 * HimeLib: the numbers of an array, nested or not ({@code [[x, y, z], ...]} as Bedrock and Blockbench write a
	 * mesh, or {@code [x, y, z, ...]}); an empty array for none.
	 */
	private static double[] flatten(@Nullable com.google.gson.JsonArray array) {
		if (array == null)
			return new double[0];

		it.unimi.dsi.fastutil.doubles.DoubleArrayList out = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();

		for (com.google.gson.JsonElement element : array) {
			if (element.isJsonArray()) {
				for (double d : flatten(element.getAsJsonArray()))
					out.add(d);
			}
			else {
				out.add(element.getAsDouble());
			}
		}

		return out.toDoubleArray();
	}
}
