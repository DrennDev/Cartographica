package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import com.drenn.cartographica.config.CartographicaConfig;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    private static final Queue<TileToGenerate> tileGenerationQueue = new PriorityQueue<>(
            Comparator.comparingDouble(t -> t.distanceFromPlayer)
    );
    private static final Set<String> tilesInQueue = new HashSet<>();

    private static int ticksSinceLastGeneration = 0;
    private static boolean worldFullyLoaded = false;
    private static int worldLoadTicks = 0;

    private static int lastPlayerTileX = Integer.MIN_VALUE;
    private static int lastPlayerTileZ = Integer.MIN_VALUE;

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() == Minecraft.getInstance().player) {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level != null) {
                String worldName = "world";
                if (mc.getCurrentServer() != null) {
                    worldName = mc.getCurrentServer().name;
                }

                String dimension = mc.level.dimension().location().getPath();

                TileManager.initialize(worldName, dimension);

                tileGenerationQueue.clear();
                tilesInQueue.clear();
                worldFullyLoaded = false;
                worldLoadTicks = 0;
                lastPlayerTileX = Integer.MIN_VALUE;
                lastPlayerTileZ = Integer.MIN_VALUE;

                Cartographica.LOGGER.info("Initialized Cartographica for world: {} dimension: {}", worldName, dimension);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player != event.getEntity()) {
            return;
        }

        // Wait for world load
        if (!worldFullyLoaded) {
            worldLoadTicks++;
            if (worldLoadTicks >= 20) {
                worldFullyLoaded = true;
                Cartographica.LOGGER.info("World loaded, starting tile generation");
                queueTilesAroundPlayer(mc);
            }
            return;
        }

        // Keybindings
        if (KeyBindings.OPEN_MAP.consumeClick()) {
            Cartographica.LOGGER.info("Opening fullscreen map!");
            mc.setScreen(new FullscreenMapScreen());
        }

        // Zoom controls for minimap
        if (KeyBindings.ZOOM_IN.consumeClick()) {
            double currentZoom = CartographicaConfig.MINIMAP_ZOOM.get();
            double newZoom = Math.min(4.0, currentZoom + 0.25);
            CartographicaConfig.MINIMAP_ZOOM.set(newZoom);
            Cartographica.LOGGER.info("Minimap zoom: {}", newZoom);
        }

        if (KeyBindings.ZOOM_OUT.consumeClick()) {
            double currentZoom = CartographicaConfig.MINIMAP_ZOOM.get();
            double newZoom = Math.max(0.5, currentZoom - 0.25);
            CartographicaConfig.MINIMAP_ZOOM.set(newZoom);
            Cartographica.LOGGER.info("Minimap zoom: {}", newZoom);
        }

        if (KeyBindings.TOGGLE_MINIMAP.consumeClick()) {
            boolean enabled = CartographicaConfig.MINIMAP_ENABLED.get();
            CartographicaConfig.MINIMAP_ENABLED.set(!enabled);
            Cartographica.LOGGER.info("Minimap: {}", !enabled ? "enabled" : "disabled");
        }

        // Check if player moved to new tile
        int playerBlockX = (int) mc.player.getX();
        int playerBlockZ = (int) mc.player.getZ();
        int playerTileX = TileManager.getTileX(playerBlockX);
        int playerTileZ = TileManager.getTileZ(playerBlockZ);

        if (playerTileX != lastPlayerTileX || playerTileZ != lastPlayerTileZ) {
            lastPlayerTileX = playerTileX;
            lastPlayerTileZ = playerTileZ;
            queueTilesAroundPlayer(mc);
        }

        // Generate tiles
        ticksSinceLastGeneration++;
        if (ticksSinceLastGeneration >= 20 && !tileGenerationQueue.isEmpty()) {
            generateNextTileFromQueue(mc);
            ticksSinceLastGeneration = 0;
        }
    }

    private static void queueTilesAroundPlayer(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        int playerBlockX = (int) mc.player.getX();
        int playerBlockZ = (int) mc.player.getZ();
        int playerTileX = TileManager.getTileX(playerBlockX);
        int playerTileZ = TileManager.getTileZ(playerBlockZ);

        // Generate in spiral (circular) pattern around player
        int radius = 2;

        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    // Only this ring
                    if (Math.abs(dx) < ring && Math.abs(dz) < ring) {
                        continue;
                    }

                    int tileX = playerTileX + dx;
                    int tileZ = playerTileZ + dz;

                    String key = tileX + "_" + tileZ;

                    if (!tilesInQueue.contains(key) && !TileManager.tileExists(tileX, tileZ)) {
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        tileGenerationQueue.add(new TileToGenerate(tileX, tileZ, distance));
                        tilesInQueue.add(key);
                    }
                }
            }
        }

        if (tileGenerationQueue.size() > 0) {
            Cartographica.LOGGER.info("Tiles queued: {}, Player at {},{}",
                    tileGenerationQueue.size(), playerTileX, playerTileZ);
        }
    }

    private static void generateNextTileFromQueue(Minecraft mc) {
        if (mc.level == null) {
            return;
        }

        TileToGenerate tile = tileGenerationQueue.poll();
        if (tile != null) {
            tilesInQueue.remove(tile.tileX + "_" + tile.tileZ);
            TileManager.generateAndSaveTile(mc.level, tile.tileX, tile.tileZ);
        }
    }

    private static class TileToGenerate {
        final int tileX;
        final int tileZ;
        final double distanceFromPlayer;

        TileToGenerate(int tileX, int tileZ, double distance) {
            this.tileX = tileX;
            this.tileZ = tileZ;
            this.distanceFromPlayer = distance;
        }
    }
}