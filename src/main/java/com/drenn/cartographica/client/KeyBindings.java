package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class KeyBindings {

    public static final String CATEGORY = "key.categories.cartographica";

    public static final KeyMapping OPEN_MAP = new KeyMapping(
            "key.cartographica.open_map",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping ZOOM_IN = new KeyMapping(
            "key.cartographica.zoom_in",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_EQUAL, // = key
            CATEGORY
    );

    public static final KeyMapping ZOOM_OUT = new KeyMapping(
            "key.cartographica.zoom_out",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_MINUS, // - key
            CATEGORY
    );

    public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping(
            "key.cartographica.toggle_minimap",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP);
        event.register(ZOOM_IN);
        event.register(ZOOM_OUT);
        event.register(TOGGLE_MINIMAP);
    }
}