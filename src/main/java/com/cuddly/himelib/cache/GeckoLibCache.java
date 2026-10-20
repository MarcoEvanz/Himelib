package com.cuddly.himelib.cache;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import com.cuddly.himelib.GeckoLibConstants;
import com.cuddly.himelib.animation.Animation;
import com.cuddly.himelib.cache.object.BakedGeoModel;
import com.cuddly.himelib.loading.FileLoader;
import com.cuddly.himelib.loading.json.raw.Model;
import com.cuddly.himelib.loading.json.typeadapter.BakedAnimationsAdapter;
import com.cuddly.himelib.loading.object.BakedAnimations;
import com.cuddly.himelib.loading.object.BakedModelFactory;
import com.cuddly.himelib.loading.object.GeometryTree;
import com.cuddly.himelib.model.GeoModel;
import com.cuddly.himelib.util.CompoundException;

import org.jetbrains.annotations.Nullable;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Cache class for holding loaded {@link Animation Animations}
 * and {@link GeoModel Models}
 */
public final class GeckoLibCache {
	private static final Set<String> EXCLUDED_NAMESPACES = ObjectOpenHashSet.of("moreplayermodels", "customnpcs", "gunsrpg");

	private static Map<ResourceLocation, BakedAnimations> ANIMATIONS = Collections.emptyMap();
	private static Map<ResourceLocation, BakedGeoModel> MODELS = Collections.emptyMap();

	public static Map<ResourceLocation, BakedAnimations> getBakedAnimations() {
		return ANIMATIONS;
	}

	public static Map<ResourceLocation, BakedGeoModel> getBakedModels() {
		return MODELS;
	}

	/**
	 * HimeLib: models and animations a mod puts in at runtime, made in memory rather than read from the resource
	 * packs (see {@link com.cuddly.himelib.loading.RuntimeLoader}). They are looked up before the reloaded ones,
	 * outlast a resource reload, and are swapped whole, so a reader on another thread sees one set or the other.
	 */
	private static volatile Map<ResourceLocation, BakedGeoModel> RUNTIME_MODELS = Collections.emptyMap();
	private static volatile Map<ResourceLocation, BakedAnimations> RUNTIME_ANIMATIONS = Collections.emptyMap();

	/** HimeLib: the runtime models and animations from now on, in place of the ones there were. */
	public static void setRuntimeAssets(Map<ResourceLocation, BakedGeoModel> models, Map<ResourceLocation, BakedAnimations> animations) {
		RUNTIME_MODELS = Map.copyOf(models);
		RUNTIME_ANIMATIONS = Map.copyOf(animations);
	}

	/** HimeLib: the model at {@code location}: a runtime one, else the resource packs' one; null when neither has it. */
	@Nullable
	public static BakedGeoModel getModel(ResourceLocation location) {
		BakedGeoModel model = RUNTIME_MODELS.get(location);

		return model != null ? model : MODELS.get(location);
	}

	/** HimeLib: the animations file at {@code location}: a runtime one, else the resource packs' one; null when neither has it. */
	@Nullable
	public static BakedAnimations getAnimations(ResourceLocation location) {
		BakedAnimations animations = RUNTIME_ANIMATIONS.get(location);

		return animations != null ? animations : ANIMATIONS.get(location);
	}

	/** HimeLib: every animations file there is, runtime ones included. */
	public static Iterable<BakedAnimations> allAnimations() {
		java.util.List<BakedAnimations> all = new java.util.ArrayList<>(RUNTIME_ANIMATIONS.values());
		all.addAll(ANIMATIONS.values());

		return all;
	}

	public static void registerReloadListener() {
		Minecraft mc = Minecraft.getInstance();

		if (mc != null && mc.getResourceManager() instanceof ReloadableResourceManager resourceManager)
			resourceManager.registerReloadListener(GeckoLibCache::reload);
	}

	public static CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
			ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor,
			Executor gameExecutor) {
		Map<ResourceLocation, BakedAnimations> animations = new Object2ObjectOpenHashMap<>();
		Map<ResourceLocation, BakedGeoModel> models = new Object2ObjectOpenHashMap<>();

		return CompletableFuture.allOf(
				loadAnimations(backgroundExecutor, resourceManager, animations::put),
				loadModels(backgroundExecutor, resourceManager, models::put))
				.thenCompose(stage::wait).thenAcceptAsync(empty -> {
					GeckoLibCache.ANIMATIONS = animations;
					GeckoLibCache.MODELS = models;
				}, gameExecutor);
	}

	private static CompletableFuture<Void> loadAnimations(Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedAnimations> elementConsumer) {
		return CompletableFuture.runAsync(() -> BakedAnimationsAdapter.COMPRESSION_CACHE = new ConcurrentHashMap<>(), backgroundExecutor)
				.thenRunAsync(() ->
									  loadResources(backgroundExecutor, resourceManager, "animations", resource -> {
										  try {
											  return FileLoader.loadAnimationsFile(resource, resourceManager);
										  }
										  catch (CompoundException ex) {
											  ex.withMessage(resource.toString() + ": Error loading animation file").printStackTrace();

											  return new BakedAnimations(new Object2ObjectOpenHashMap<>());
										  }
										  catch (Exception ex) {
											  throw GeckoLibConstants.exception(resource, "Error loading animation file", ex);
										  }
									  }, elementConsumer))
				.thenRunAsync(() -> BakedAnimationsAdapter.COMPRESSION_CACHE = null);
	}

	private static CompletableFuture<Void> loadModels(Executor backgroundExecutor, ResourceManager resourceManager, BiConsumer<ResourceLocation, BakedGeoModel> elementConsumer) {
		return loadResources(backgroundExecutor, resourceManager, "geo", resource -> {
			try {
				Model model = FileLoader.loadModelFile(resource, resourceManager);

				switch (model.formatVersion()) {
					case V_1_12_0 -> {}
					case V_1_14_0 -> GeckoLibConstants.LOGGER.warn("Unsupported geometry json version: 1.14.0 for model {}. This model may not appear as expected", resource);
					case V_1_21_0 -> GeckoLibConstants.LOGGER.warn("Unsupported geometry json version: 1.21.0 for model {}. Supported versions: 1.12.0. Remove any rotated face UVs and re-export the model to fix", resource);
					case null, default -> GeckoLibConstants.LOGGER.warn("Unsupported geometry json version for model {}. Supported versions: 1.12.0", resource);
				}

				return BakedModelFactory.getForNamespace(resource.getNamespace()).constructGeoModel(GeometryTree.fromModel(model));
			}
			catch (Exception ex) {
				throw GeckoLibConstants.exception(resource, "Error loading model file", ex);
			}
		}, elementConsumer);
	}

	private static <T> CompletableFuture<Void> loadResources(Executor executor, ResourceManager resourceManager,
			String path, Function<ResourceLocation, T> loader, BiConsumer<ResourceLocation, T> map) {
		return CompletableFuture.supplyAsync(
				() -> resourceManager.listResources(path, fileName -> fileName.toString().endsWith(".json")), executor)
				.thenApplyAsync(resources -> {
					Map<ResourceLocation, CompletableFuture<T>> tasks = new Object2ObjectOpenHashMap<>();

					for (ResourceLocation resource : resources.keySet()) {
						tasks.put(resource, CompletableFuture.supplyAsync(() -> loader.apply(resource), executor));
					}

					return tasks;
				}, executor)
				.thenAcceptAsync(tasks -> {
					for (Entry<ResourceLocation, CompletableFuture<T>> entry : tasks.entrySet()) {
						// Skip known namespaces that use an "animation" or "geo" folder as well
						if (!EXCLUDED_NAMESPACES.contains(entry.getKey().getNamespace().toLowerCase(Locale.ROOT)))
							map.accept(entry.getKey(), entry.getValue().join());
					}
				}, executor);
	}

	/**
	 * Register a new namespace to be excluded from GeckoLib's resource loader
	 *
	 * @param namespace The namespace to exclude
	 */
	public static synchronized void registerNamespaceExclusion(String namespace) {
		EXCLUDED_NAMESPACES.add(namespace);
	}
}
