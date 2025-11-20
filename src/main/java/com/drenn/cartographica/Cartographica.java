package com.drenn.cartographica;

import com.drenn.cartographica.config.CartographicaConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Cartographica.MOD_ID)
public class Cartographica {

    public static final String MOD_ID = "cartographica";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cartographica(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Cartographica is initializing...");

        // Register config
        modContainer.registerConfig(ModConfig.Type.CLIENT, CartographicaConfig.SPEC);
        LOGGER.info("Config registered");
    }
}