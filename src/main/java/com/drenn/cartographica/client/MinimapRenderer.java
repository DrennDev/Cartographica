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
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Cartographica.MOD_ID, value = Dist.CLIENT)
public class MinimapRenderer {

    private static final float BLOCKS_PER_PIXEL = 1.0f;

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

        // Calculate minimap position (top-right corner)
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int minimapX = screenWidth - minimapSize - margin;
        int minimapY = margin;

        // Draw the minimap background
        int backgroundColor = 0x80000000;
        graphics.fill(minimapX, minimapY, minimapX + minimapSize, minimapY + minimapSize, backgroundColor);

        // Draw border
        int borderColor = 0xFFFFFFFF;
        drawBorder(graphics, minimapX, minimapY, minimapSize, borderColor);

        // Draw the player arrow
        drawPlayerArrow(graphics, mc, minimapX, minimapY, minimapSize);

        // Optionally show coordinates
        if (showCoords) {
            drawCoordinates(graphics, mc, minimapX, minimapY, minimapSize);
        }
    }

    private static void drawPlayerArrow(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc,
                                        int minimapX, int minimapY, int minimapSize) {
        // Player is always in the center
        int centerX = minimapX + minimapSize / 2;
        int centerY = minimapY + minimapSize / 2;

        // Get player's rotation (yaw)
        float yaw = mc.player.getYRot();

        // Get the PoseStack for rotation
        PoseStack poseStack = graphics.pose();

        // Save the current state
        poseStack.pushPose();

        // Move to the center point
        poseStack.translate(centerX, centerY, 0);

        // Rotate around this point
        // Note: Minecraft's yaw is backwards from normal rotation, and we add 180 to point "forward"
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(yaw + 180));

        // Now draw a triangle pointing "up" (which will be rotated to face the player's direction)
        int arrowSize = 5;
        int arrowColor = 0xFFFF0000;  // Red

        // Triangle points (pointing upward in local space)
        // Top point
        int x1 = 0;
        int y1 = -arrowSize;
        // Bottom-left point
        int x2 = -arrowSize;
        int y2 = arrowSize;
        // Bottom-right point
        int x3 = arrowSize;
        int y3 = arrowSize;

        // Draw the filled triangle
        drawTriangle(graphics, x1, y1, x2, y2, x3, y3, arrowColor);

        // Restore the previous state
        poseStack.popPose();
    }

    private static void drawTriangle(net.minecraft.client.gui.GuiGraphics graphics,
                                     int x1, int y1, int x2, int y2, int x3, int y3, int color) {
        // Simple triangle fill using scanline algorithm
        // Find bounding box
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));

        // For each pixel in bounding box, check if it's inside the triangle
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (isPointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
                    graphics.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static boolean isPointInTriangle(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
        // Use barycentric coordinates to check if point is inside triangle
        float denom = ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
        if (denom == 0) return false;

        float a = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / denom;
        float b = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / denom;
        float c = 1 - a - b;

        return a >= 0 && a <= 1 && b >= 0 && b <= 1 && c >= 0 && c <= 1;
    }

    private static void drawCoordinates(net.minecraft.client.gui.GuiGraphics graphics, Minecraft mc,
                                        int minimapX, int minimapY, int minimapSize) {
        Vec3 pos = mc.player.position();
        String coords = String.format("X: %d Y: %d Z: %d", (int)pos.x, (int)pos.y, (int)pos.z);

        // Draw text below the minimap
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