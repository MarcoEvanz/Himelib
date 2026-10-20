package com.cuddly.himelib;

import net.fabricmc.api.ModInitializer;
import com.cuddly.himelib.service.GeckoLibNetworking;

/**
 * Main GeckoLib entrypoint
 */
public final class GeckoLib implements ModInitializer {
    @Override
    public void onInitialize() {
        GeckoLibConstants.init();
        GeckoLibNetworking.init();
    }
}
