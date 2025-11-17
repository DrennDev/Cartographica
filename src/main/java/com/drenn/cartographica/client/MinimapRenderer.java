package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import com.drenn.cartographica.config.CartographicaConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.awt.image.BufferedImage;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class MinimapRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.screen != null) {
            return;
        }

        var graphics = event.getGuiGraphics();

        // Get config values
        int minimapSize = CartographicaConfig.MINIMAP_SIZE.get();
        int margin = CartographicaConfig.MINIMAP_MARGIN.get();
        boolean showCoords = CartographicaConfig.SHOW_COORDINATES.get();

        // Calculate minimap position
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int minimapX = screenWidth - minimapSize - margin;
        int minimapY = margin;

        // Draw background
        int backgroundColor = 0x80000000;
        graphics.fill(minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize, backgroundColor);

        // Draw terrain from tiles (FAST!)
        drawTerrainFromTiles(graphics, mc, minimapX, minimapY, minimapSize);

        // Draw border
        int borderColor = 0xFFFFFFFF;
        drawBorder(graphics, minimapX, minimapY, minimapSize, borderColor);

        // Draw player arrow
        drawPlayerArrow(graphics, mc, minimapX, minimapY, minimapSize);

        // Optionally show coordinates
        if (showCoords) {
            drawCoordinates(graphics, mc, minimapX, minimapY, minimapSize);
        }
    }

    private static void drawTerrainFromTiles(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc,
                                             int minimapX, int minimapY, int minimapSize) {
        if (mc.level == null || mc.player == null) {
            return;
        }

        // Get player position
        int playerBlockX = (int) mc.player.getX();
        int playerBlockZ = (int) mc.player.getZ();

        // For each pixel on the minimap, sample from the appropriate tile
        int radius = minimapSize / 2;

        for (int dx = -radius; dx < radius; dx++) {
            for (int dz = -radius; dz < radius; dz++) {
                // World position for this pixel
                int worldX = playerBlockX + dx;
                int worldZ = playerBlockZ + dz;

                // Which tile contains this block?
                int tileX = TileManager.getTileX(worldX);
                int tileZ = TileManager.getTileZ(worldZ);

                // Load the tile
                BufferedImage tile = TileManager.loadTile(tileX, tileZ);
                if (tile == null) {
                    continue; // Tile not generated yet
                }

                // Which pixel in the tile?
                int tilePixelX = Math.floorMod(worldX, TileManager.TILE_SIZE);
                int tilePixelZ = Math.floorMod(worldZ, TileManager.TILE_SIZE);

                // Get color from tile
                int color = tile.getRGB(tilePixelX, tilePixelZ);

                // Draw on minimap
                int minimapPixelX = minimapX + radius + dx;
                int minimapPixelZ = minimapY + radius + dz;

                if (color != 0) { // Don't draw fully transparent
                    graphics.fill(minimapPixelX, minimapPixelZ, minimapPixelX + 1, minimapPixelZ + 1, color);
                }
            }
        }
    }

    private static void drawPlayerArrow(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc,
                                        int minimapX, int minimapY, int minimapSize) {
        int centerX = minimapX + minimapSize / 2;
        int centerY = minimapY + minimapSize / 2;
        float yaw = mc.player.getYRot();

        CartographicaConfig.PlayerIndicatorType indicatorType =
                CartographicaConfig.PLAYER_INDICATOR_TYPE.get();

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(yaw + 180));

        switch (indicatorType) {
            case SIMPLE_TRIANGLE -> drawSimpleTriangle(graphics);
            case ARROW_SHAPE -> drawArrowShape(graphics);
            case TEXTURE -> drawTextureArrow(graphics, mc);
        }

        poseStack.popPose();
    }

    private static void drawSimpleTriangle(net.minecraft.client.gui.GuiGraphics graphics) {
        int arrowSize = 5;
        int arrowColor = 0xFFFF0000;
        drawTriangle(graphics, 0, -arrowSize, -arrowSize, arrowSize, arrowSize, arrowSize, arrowColor);
    }

    private static void drawArrowShape(net.minecraft.client.gui.GuiGraphics graphics) {
        int arrowColor = 0xFFFF0000;
        int x1 = 0, y1 = -7;
        int x2 = 5, y2 = 2;
        int x3 = 0, y3 = -1;
        int x4 = -5, y4 = 2;

        drawTriangle(graphics, x1, y1, x2, y2, x3, y3, arrowColor);
        drawTriangle(graphics, x1, y1, x3, y3, x4, y4, arrowColor);
        drawArrowOutline(graphics, x1, y1, x2, y2, x3, y3, x4, y4, 0xFFFFFFFF);
    }

    private static void drawTextureArrow(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc) {
        int size = 16;
        graphics.blit(
                TextureHelper.PLAYER_ARROW,
                -size / 2, -size / 2,
                0, 0,
                size, size,
                size, size
        );
    }

    private static void drawTriangle(net.minecraft.client.gui.GuiGraphics graphics,
                                     int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isPointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static boolean isPointInTriangle(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
        float denom = ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (denom == 0) return false;

        float a = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / denom;
        float b = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / denom;
        float c = 1 - a - b;

        return a >= 0 && a <= 1 && b >= 0 && b <= 1 && c >= 0 && c <= 1;
    }

    private static void drawArrowOutline(net.minecraft.client.gui.GuiGraphics graphics,
                                         int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4, int color) {
        drawLine(graphics, x1, y1, x2, y2, color);
        drawLine(graphics, x2, y2, x3, y3, color);
        drawLine(graphics, x3, y3, x4, y4, color);
        drawLine(graphics, x4, y4, x1, y1, color);
    }

    private static void drawLine(net.minecraft.client.gui.GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            graphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private static void drawCoordinates(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc,
                                        int minimapX, int minimapY, int minimapSize) {
        Vec3 pos = mc.player.position();
        String coords = String.format("X: %d Y: %d Z: %d", (int)pos.x, (int)pos.y, (int)pos.z);

        int textX = minimapX;
        int textY = minimapY + minimapSize + 2;
        graphics.drawString(mc.font, coords, textX, textY, 0xFFFFFFFF, true);
    }

    private static void drawBorder(net.minecraft.client.gui.GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }
}