package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import com.drenn.cartographica.config.CartographicaConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class MinimapRenderer {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<String, ResourceLocation> tileTextureCache = new HashMap<>();
    private static int nextTextureId = 0;

    /**
     * Render the minimap HUD overlay
     */
    public static void render(GuiGraphics graphics, float partialTick) {
        if (!CartographicaConfig.MINIMAP_ENABLED.get()) {
            return;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        // Get config settings
        int size = CartographicaConfig.MINIMAP_SIZE.get();
        CartographicaConfig.MinimapPosition position = CartographicaConfig.MINIMAP_POSITION.get();
        int margin = CartographicaConfig.MINIMAP_MARGIN.get();

        // Calculate position on screen
        int x = calculateX(position, size, margin);
        int y = calculateY(position, size, margin);

        // Render based on shape
        if (CartographicaConfig.MINIMAP_SHAPE.get() == CartographicaConfig.MinimapShape.CIRCLE) {
            renderCircularMinimap(graphics, x, y, size, partialTick);
        } else {
            renderSquareMinimap(graphics, x, y, size, partialTick);
        }

        // Render overlays (coordinates, time, etc.)
        renderOverlays(graphics, x, y, size);
    }

    /**
     * Render square minimap
     */
    private static void renderSquareMinimap(GuiGraphics graphics, int x, int y, int size, float partialTick) {
        Player player = mc.player;

        // Draw background
        graphics.fill(x, y, x + size, y + size, 0xAA000000);

        // Render map tiles
        renderMapTiles(graphics, x, y, size, partialTick);

        // Draw border
        if (CartographicaConfig.SHOW_BORDER.get()) {
            int borderColor = CartographicaConfig.BORDER_COLOR.get();
            int borderWidth = CartographicaConfig.BORDER_WIDTH.get();

            for (int i = 0; i < borderWidth; i++) {
                // Top
                graphics.fill(x - i, y - i, x + size + i, y - i + 1, borderColor);
                // Bottom
                graphics.fill(x - i, y + size + i - 1, x + size + i, y + size + i, borderColor);
                // Left
                graphics.fill(x - i, y - i, x - i + 1, y + size + i, borderColor);
                // Right
                graphics.fill(x + size + i - 1, y - i, x + size + i, y + size + i, borderColor);
            }
        }

        // Draw player marker at center
        renderPlayerMarker(graphics, x + size / 2, y + size / 2);
    }

    /**
     * Render circular minimap (with masking)
     */
    private static void renderCircularMinimap(GuiGraphics graphics, int x, int y, int size, float partialTick) {
        // TODO: Implement circular masking using stencil buffer
        // For now, use square
        renderSquareMinimap(graphics, x, y, size, partialTick);
    }

    /**
     * Render map tiles in the minimap area
     */
    private static void renderMapTiles(GuiGraphics graphics, int screenX, int screenY, int size, float partialTick) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();
        double zoom = CartographicaConfig.MINIMAP_ZOOM.get();

        // How many blocks fit in the minimap?
        int blocksVisible = (int) (size / zoom);

        // Calculate world bounds
        int minWorldX = (int) (playerX - blocksVisible / 2);
        int maxWorldX = (int) (playerX + blocksVisible / 2);
        int minWorldZ = (int) (playerZ - blocksVisible / 2);
        int maxWorldZ = (int) (playerZ + blocksVisible / 2);

        // Calculate tile bounds
        int minTileX = TileManager.getTileX(minWorldX);
        int maxTileX = TileManager.getTileX(maxWorldX);
        int minTileZ = TileManager.getTileZ(minWorldZ);
        int maxTileZ = TileManager.getTileZ(maxWorldZ);

        // Render each tile
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                renderMinimapTile(graphics, tileX, tileZ, screenX, screenY, size, playerX, playerZ, zoom);
            }
        }
    }

    /**
     * Render a single tile on the minimap
     */
    private static void renderMinimapTile(GuiGraphics graphics, int tileX, int tileZ,
                                          int screenX, int screenY, int size,
                                          double playerX, double playerZ, double zoom) {
        // Get tile texture
        ResourceLocation texture = getTileTexture(tileX, tileZ);
        if (texture == null) {
            return;
        }

        // Calculate tile world position
        double tileWorldX = tileX * TileManager.TILE_SIZE;
        double tileWorldZ = tileZ * TileManager.TILE_SIZE;

        // Convert to minimap-relative coordinates
        double relativeX = tileWorldX - playerX;
        double relativeZ = tileWorldZ - playerZ;

        // Convert to screen coordinates
        int tileScreenX = screenX + size / 2 + (int) (relativeX * zoom);
        int tileScreenY = screenY + size / 2 + (int) (relativeZ * zoom);
        int tileScreenSize = (int) (TileManager.TILE_SIZE * zoom);

        // Clip to minimap bounds
        if (tileScreenX + tileScreenSize < screenX || tileScreenX > screenX + size ||
                tileScreenY + tileScreenSize < screenY || tileScreenY > screenY + size) {
            return; // Completely outside
        }

        // Draw the tile
        graphics.blit(
                texture,
                tileScreenX, tileScreenY,
                tileScreenSize, tileScreenSize,
                0.0F, 0.0F,
                TileManager.TILE_SIZE, TileManager.TILE_SIZE,
                TileManager.TILE_SIZE, TileManager.TILE_SIZE
        );
    }

    /**
     * Get or load tile texture
     */
    private static ResourceLocation getTileTexture(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;

        if (tileTextureCache.containsKey(key)) {
            return tileTextureCache.get(key);
        }

        // Load tile image
        BufferedImage tileImage = TileManager.loadTile(tileX, tileZ);
        if (tileImage == null) {
            return null;
        }

        // Convert to NativeImage and upload to GPU
        NativeImage nativeImage = new NativeImage(TileManager.TILE_SIZE, TileManager.TILE_SIZE, false);

        for (int x = 0; x < TileManager.TILE_SIZE; x++) {
            for (int y = 0; y < TileManager.TILE_SIZE; y++) {
                int rgb = tileImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xFF;
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                nativeImage.setPixelRGBA(x, y, abgr);
            }
        }

        DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                Cartographica.MOD_ID,
                "minimap_tile_" + nextTextureId++
        );

        mc.getTextureManager().register(location, dynamicTexture);
        tileTextureCache.put(key, location);

        return location;
    }

    /**
     * Render player marker
     */
    private static void renderPlayerMarker(GuiGraphics graphics, int centerX, int centerY) {
        CartographicaConfig.PlayerMarkerType type = CartographicaConfig.PLAYER_MARKER_TYPE.get();
        int color = CartographicaConfig.PLAYER_MARKER_COLOR.get();
        int size = CartographicaConfig.PLAYER_MARKER_SIZE.get();

        switch (type) {
            case ARROW:
                renderArrowMarker(graphics, centerX, centerY, size, color);
                break;
            case DOT:
                renderDotMarker(graphics, centerX, centerY, size, color);
                break;
            case CROSSHAIR:
                renderCrosshairMarker(graphics, centerX, centerY, size, color);
                break;
            case SURVEYOR:
                renderSurveyorMarker(graphics, centerX, centerY, size, color);
                break;
        }
    }

    private static void renderArrowMarker(GuiGraphics graphics, int x, int y, int size, int color) {
        // Simple triangle pointing up
        graphics.fill(x, y - size / 2, x + 1, y + size / 2, color);
        graphics.fill(x - size / 2, y, x + size / 2, y + 1, color);
    }

    private static void renderDotMarker(GuiGraphics graphics, int x, int y, int size, int color) {
        // Simple square dot
        int half = size / 2;
        graphics.fill(x - half, y - half, x + half, y + half, color);
    }

    private static void renderCrosshairMarker(GuiGraphics graphics, int x, int y, int size, int color) {
        // Crosshair
        graphics.fill(x - size, y, x + size, y + 1, color);
        graphics.fill(x, y - size, x + 1, y + size, color);
    }

    private static void renderSurveyorMarker(GuiGraphics graphics, int x, int y, int size, int color) {
        // Surveyor crosshair (like you designed before!)
        int armLength = size;
        int gap = size / 2;
        int thickness = 2;

        // Top
        graphics.fill(x - thickness / 2, y - armLength, x + thickness / 2, y - gap, color);
        // Bottom
        graphics.fill(x - thickness / 2, y + gap, x + thickness / 2, y + armLength, color);
        // Left
        graphics.fill(x - armLength, y - thickness / 2, x - gap, y + thickness / 2, color);
        // Right
        graphics.fill(x + gap, y - thickness / 2, x + armLength, y + thickness / 2, color);
    }

    /**
     * Render overlay information (coordinates, time, etc.)
     */
    private static void renderOverlays(GuiGraphics graphics, int x, int y, int size) {
        int textY = y + size + 5;

        if (CartographicaConfig.SHOW_COORDINATES.get() && mc.player != null) {
            String coords = String.format("X: %.0f, Y: %.0f, Z: %.0f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            graphics.drawString(mc.font, coords, x, textY, 0xFFFFFFFF, true);
            textY += 12;
        }

        if (CartographicaConfig.SHOW_TIME.get() && mc.level != null) {
            long time = mc.level.getDayTime() % 24000;
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            String timeStr = String.format("Time: %02d:%02d", hours, minutes);
            graphics.drawString(mc.font, timeStr, x, textY, 0xFFFFFFFF, true);
            textY += 12;
        }

        if (CartographicaConfig.SHOW_BIOME.get() && mc.player != null && mc.level != null) {
            var biome = mc.level.getBiome(mc.player.blockPosition());
            String biomeName = biome.unwrapKey().map(key -> key.location().getPath()).orElse("Unknown");
            graphics.drawString(mc.font, "Biome: " + biomeName, x, textY, 0xFFFFFFFF, true);
            textY += 12;
        }

        if (CartographicaConfig.SHOW_FPS.get()) {
            String fps = mc.fpsString.split(" ")[0] + " FPS";
            graphics.drawString(mc.font, fps, x, textY, 0xFFFFFFFF, true);
        }
    }

    /**
     * Calculate X position based on corner setting
     */
    private static int calculateX(CartographicaConfig.MinimapPosition position, int size, int margin) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        return switch (position) {
            case TOP_LEFT, BOTTOM_LEFT -> margin;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - size - margin;
        };
    }

    /**
     * Calculate Y position based on corner setting
     */
    private static int calculateY(CartographicaConfig.MinimapPosition position, int size, int margin) {
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        return switch (position) {
            case TOP_LEFT, TOP_RIGHT -> margin;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - size - margin;
        };
    }
}