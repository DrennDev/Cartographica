package com.drenn.cartographica;

import com.drenn.cartographica.client.KeyBindings;
import com.drenn.cartographica.config.CartographicaConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Cartographica.MOD_ID)
public class Cartographica {
    public static final String MOD_ID = "cartographica";
    public static final Logger LOGGER = LogManager.getLogger();

    public Cartographica(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.CLIENT, CartographicaConfig.SPEC);

        modEventBus.addListener(this::registerKeyBindings);

        LOGGER.info("Cartographica mod is loading!");
        LOGGER.info("Hello from Drenn's Cartographica!");
    }
    private void registerKeyBindings(RegisterKeyMappingsEvent event) {
        LOGGER.info("Registering keybindings...");
        event.register(KeyBindings.OPEN_MAP);
        LOGGER.info("Keybindings registered!");
    }
}