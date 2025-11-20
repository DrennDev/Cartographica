package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class ChunkUpdateListener {

    /**
     * Called when a chunk is loaded client-side
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (event.getChunk() instanceof LevelChunk levelChunk) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Update this chunk in the tile (real-time!)
                TileManager.updateChunkInTile(mc.level, levelChunk);
            }
        }
    }

    /**
     * Called when a chunk is sent to the player (real-time updates!)
     */
    @SubscribeEvent
    public static void onChunkSent(ChunkWatchEvent.Sent event) {
        LevelChunk chunk = event.getChunk();
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && mc.level.isClientSide()) {
            // Update this chunk immediately
            TileManager.updateChunkInTile(mc.level, chunk);
            Cartographica.LOGGER.debug("Real-time chunk update: {}, {}", chunk.getPos().x, chunk.getPos().z);
        }
    }
}