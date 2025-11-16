package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.security.Key;

public class ClientEvents {
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()){
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != event.getEntity()) {
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
