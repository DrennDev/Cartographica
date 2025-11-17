package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.security.Key;
@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (KeyBindings.OPEN_MAP.consumeClick()) {
            Cartographica.LOGGER.info("M key was pressed!");
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Map key pressed!")
            );
        }
    }
}
