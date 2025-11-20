package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.DeltaTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class MinimapHudOverlay {

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiLayerEvent.Post event) {
        // Get partial tick from DeltaTracker
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        // Render minimap on top of everything
        MinimapRenderer.render(event.getGuiGraphics(), partialTick);
    }
}