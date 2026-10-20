package com.cuddly.himelib.platform;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.cuddly.himelib.animatable.GeoAnimatable;
import com.cuddly.himelib.animatable.client.GeoRenderProvider;
import com.cuddly.himelib.model.GeoModel;
import com.cuddly.himelib.renderer.GeoArmorRenderer;
import com.cuddly.himelib.renderer.GeoRenderer;
import com.cuddly.himelib.service.GeckoLibClient;

/**
 * Fabric service implementation for clientside functionalities
 */
public class GeckoLibClientFabric implements GeckoLibClient {
    /**
     * Helper method for retrieving an (ideally) cached instance of the armor model for a given Item
     * <p>
     * If no custom model applies to this item, the {@code defaultModel} is returned
     */
    @NotNull
    @Override
    public <T extends LivingEntity & GeoAnimatable> HumanoidModel<?> getArmorModelForItem(T animatable, ItemStack stack, EquipmentSlot slot, HumanoidModel<LivingEntity> defaultModel) {
        return GeoRenderProvider.of(stack).getGeoArmorRenderer(animatable, stack, slot, defaultModel) instanceof GeoArmorRenderer<?> geoArmorRenderer ? geoArmorRenderer : defaultModel;
    }

    /**
     * Helper method for retrieving an (ideally) cached instance of the GeoModel for the given item
     *
     * @return The GeoModel for the item, or null if not applicable
     */
    @Nullable
    @Override
    public GeoModel<?> getGeoModelForItem(ItemStack item) {
        if (GeoRenderProvider.of(item).getGeoItemRenderer() instanceof GeoRenderer<?> geoItemRenderer)
            return geoItemRenderer.getGeoModel();

        return null;
    }

    /**
     * Helper method for retrieving an (ideally) cached instance of the GeoModel for the given item's armor renderer
     *
     * @return The GeoModel for the item, or null if not applicable
     */
    @Nullable
    @Override
    public GeoModel<?> getGeoModelForArmor(ItemStack armour) {
        if (GeoRenderProvider.of(armour).getGeoArmorRenderer(null, armour, null, null) instanceof GeoArmorRenderer<?> armorRenderer)
            return armorRenderer.getGeoModel();

        return null;
    }
}
