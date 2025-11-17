package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.security.Key;
@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Handle keybinding
        if (KeyBindings.OPEN_MAP.consumeClick()) {
            Cartographica.LOGGER.info("M key was pressed!");
            mc.player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Map key pressed!")
            );
        }

        // Generate tiles around player (only check every 20 ticks = 1 second)
        if (mc.level.getGameTime() % 20 == 0) {
            generateTilesAroundPlayer(mc);
        }
    }

    private static void generateTilesAroundPlayer(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        // Get player chunk position
        int playerChunkX = mc.player.chunkPosition().x;
        int playerChunkZ = mc.player.chunkPosition().z;

        // Generate tiles for loaded chunks (typically 8 chunk radius = 16x16 area)
        int chunkRadius = 8;

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;

                // Check if chunk is loaded before generating tile
                if (mc.level.hasChunk(chunkX, chunkZ)) {
                    // With 16x16 tiles, tile coordinates = chunk coordinates
                    TileManager.ensureTileExists(mc.level, chunkX, chunkZ);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        // Only run on client side for the player
        if (event.getLevel().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level != null) {
                // Get world name
                String worldName = "world"; // Default for single player
                if (mc.getCurrentServer() != null) {
                    worldName = mc.getCurrentServer().name;
                }

                // Get dimension (overworld, nether, end)
                String dimension = mc.level.dimension().location().getPath();

                // Initialize tile system
                TileManager.initialize(worldName, dimension);

                Cartographica.LOGGER.info("Initialized Cartographica for world: {} dimension: {}", worldName, dimension);
            }
        }
    }
}