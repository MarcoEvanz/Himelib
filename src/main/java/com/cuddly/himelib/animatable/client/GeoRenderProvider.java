package com.cuddly.himelib.animatable.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.animatable.GeoItem;
import com.cuddly.himelib.renderer.GeoArmorRenderer;
import com.cuddly.himelib.renderer.GeoItemRenderer;

/**
 * Internal interface for safely providing a custom item and armor renderer instances at runtime
 * <p>
 * This can be safely instantiated as a new anonymous class inside your {@link Item} class
 * <p>
 * This is a functional equivalent to Forge's {@code IClientItemExtensions}. If on Forge, either can be used
 *
 * @see com.cuddly.himelib.renderer.GeoItemRenderer GeoItemRenderer
 * @see com.cuddly.himelib.renderer.GeoArmorRenderer GeoArmorRenderer
 */
public interface GeoRenderProvider {
    GeoRenderProvider DEFAULT = new GeoRenderProvider() {};

    /**
     * Get the GeoRenderProvider instance for the given ItemStack, if applicable
     *
     * @param itemStack The ItemStack to get the provider instance for
     * @return The GeoRenderProvider instance for this stack, or a defaulted empty instance if not defined
     */
    static GeoRenderProvider of(ItemStack itemStack) {
        return of(itemStack.getItem());
    }

    /**
     * Get the GeoRenderProvider instance for the given Item, if applicable
     *
     * @param item The ItemStack to get the provider instance for
     * @return The GeoRenderProvider instance for this item, or a defaulted empty instance if not defined
     */
    static GeoRenderProvider of(Item item) {
        if (item instanceof GeoItem geoItem)
            return (GeoRenderProvider)geoItem.getRenderProvider();

        return DEFAULT;
    }

    /**
     * Get the cached {@link BlockEntityWithoutLevelRenderer} instance for this provider.
     * <p>
     * Normally this would be an instance of {@link GeoItemRenderer}
     *
     * @return The cached BEWLR instance for this provider, or null if not applicable
     */
    @Nullable
    default BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
        return null;
    }

    /**
     * Get the cached {@link GeoArmorRenderer} instance for this provider.
     * <p>
     * Normally this would be an instance of {@link GeoArmorRenderer}
     *
     * @param livingEntity The entity currently wearing the item, if applicable
     * @param itemStack The ItemStack for this provider
     * @param equipmentSlot The slot the ItemStack is currently in, if applicable
     * @param original The base HumanoidModel (usually the default vanilla armor model), if applicable
     * @return The cached HumanoidModel instance for this provider, or null if not applicable
     */
    @Nullable
    default <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(@Nullable T livingEntity, ItemStack itemStack, @Nullable EquipmentSlot equipmentSlot, @Nullable HumanoidModel<T> original) {
        return null;
    }
}