package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class FullscreenMapScreen extends Screen {

    // Camera in TILE coordinates (not world blocks!)
    private double cameraTileX;
    private double cameraTileZ;
    private double zoom = 1.0;

    private boolean isDragging = false;
    private double lastMouseX;
    private double lastMouseZ;

    private final Map<String, TileTextureData> tileTextures = new HashMap<>();
    private int nextTextureId = 0;

    public FullscreenMapScreen() {
        super(Component.literal("Cartographica Map"));
    }

    @Override
    protected void init() {
        super.init();

        if (minecraft != null && minecraft.player != null) {
            // Initialize camera in TILE coordinates
            double playerX = minecraft.player.getX();
            double playerZ = minecraft.player.getZ();
            this.cameraTileX = playerX / TileManager.TILE_SIZE;
            this.cameraTileZ = playerZ / TileManager.TILE_SIZE;

            Cartographica.LOGGER.info("Opened map at tile position: ({}, {})", cameraTileX, cameraTileZ);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        renderMapTiles(graphics);
        renderPlayerMarker(graphics);
        renderUI(graphics);
    }

    private void renderMapTiles(GuiGraphics graphics) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        // How many screen pixels does one tile occupy?
        double tileScreenSize = TileManager.TILE_SIZE * zoom;

        // How many tiles fit on screen?
        int tilesVisibleWidth = (int) Math.ceil(this.width / tileScreenSize) + 2;
        int tilesVisibleHeight = (int) Math.ceil(this.height / tileScreenSize) + 2;

        // Center tile (what the camera is looking at)
        int centerTileX = (int) Math.floor(cameraTileX);
        int centerTileZ = (int) Math.floor(cameraTileZ);

        // Calculate range of tiles to draw
        int minTileX = centerTileX - tilesVisibleWidth / 2 - 1;
        int maxTileX = centerTileX + tilesVisibleWidth / 2 + 1;
        int minTileZ = centerTileZ - tilesVisibleHeight / 2 - 1;
        int maxTileZ = centerTileZ + tilesVisibleHeight / 2 + 1;

        // Draw grid of tiles
        for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
            for (int tileZ = minTileZ; tileZ <= maxTileZ; tileZ++) {
                renderSingleTile(graphics, tileX, tileZ);
            }
        }
    }

    private void renderSingleTile(GuiGraphics graphics, int tileX, int tileZ) {
        TileTextureData textureData = getTileTexture(tileX, tileZ);
        if (textureData == null) {
            return;
        }

        // Calculate tile position in TILE GRID coordinates
        double relativeTileX = tileX - cameraTileX;
        double relativeTileZ = tileZ - cameraTileZ;

        // Convert to screen pixels
        double tileScreenSize = TileManager.TILE_SIZE * zoom;

        int screenX = (int) (this.width / 2 + relativeTileX * tileScreenSize);
        int screenY = (int) (this.height / 2 + relativeTileZ * tileScreenSize);
        int screenWidth = (int) tileScreenSize;
        int screenHeight = (int) tileScreenSize;

        // Use the PoseStack directly for proper texture rendering
        graphics.pose().pushPose();

        // Draw using the proper blit method with FLOAT parameters for scaling
        graphics.blit(
                textureData.location,     // Texture
                screenX,                  // Screen X
                screenY,                  // Screen Y
                screenWidth,              // Screen Width
                screenHeight,             // Screen Height
                0.0F,                     // U (texture coordinate)
                0.0F,                     // V (texture coordinate)
                TileManager.TILE_SIZE,    // Texture region width
                TileManager.TILE_SIZE,    // Texture region height
                TileManager.TILE_SIZE,    // Full texture width
                TileManager.TILE_SIZE     // Full texture height
        );

        graphics.pose().popPose();
    }

    private TileTextureData getTileTexture(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;

        if (tileTextures.containsKey(key)) {
            return tileTextures.get(key);
        }

        BufferedImage tileImage = TileManager.loadTile(tileX, tileZ);
        if (tileImage == null) {
            return null;
        }

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

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                Cartographica.MOD_ID,
                "tile_" + tileX + "_" + tileZ
        );

        DynamicTexture texture = new DynamicTexture(nativeImage);
        minecraft.getTextureManager().register(location, texture);

        TileTextureData data = new TileTextureData(location, texture, tileX, tileZ);
        tileTextures.put(key, data);

        return data;
    }

    private void renderPlayerMarker(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        // Player position in tile coordinates
        double playerTileX = minecraft.player.getX() / TileManager.TILE_SIZE;
        double playerTileZ = minecraft.player.getZ() / TileManager.TILE_SIZE;

        // Relative to camera
        double relativeTileX = playerTileX - cameraTileX;
        double relativeTileZ = playerTileZ - cameraTileZ;

        // To screen pixels
        double tileScreenSize = TileManager.TILE_SIZE * zoom;
        int screenX = (int) (this.width / 2 + relativeTileX * tileScreenSize);
        int screenY = (int) (this.height / 2 + relativeTileZ * tileScreenSize);

        int markerSize = 8;

        // Red square
        graphics.fill(
                screenX - markerSize / 2,
                screenY - markerSize / 2,
                screenX + markerSize / 2,
                screenY + markerSize / 2,
                0xFFFF0000
        );

        // White outline
        graphics.fill(screenX - 4, screenY - 4, screenX + 4, screenY - 3, 0xFFFFFFFF);
        graphics.fill(screenX - 4, screenY + 3, screenX + 4, screenY + 4, 0xFFFFFFFF);
        graphics.fill(screenX - 4, screenY - 4, screenX - 3, screenY + 4, 0xFFFFFFFF);
        graphics.fill(screenX + 3, screenY - 4, screenX + 4, screenY + 4, 0xFFFFFFFF);
    }

    private void renderUI(GuiGraphics graphics) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        graphics.drawString(minecraft.font,
                String.format("Camera Tile: %.2f, %.2f", cameraTileX, cameraTileZ),
                10, 10, 0xFFFFFFFF, true);

        graphics.drawString(minecraft.font,
                String.format("Zoom: %.2fx", zoom),
                10, 22, 0xFFFFFFFF, true);

        graphics.drawString(minecraft.font,
                String.format("Tiles Loaded: %d", tileTextures.size()),
                10, 34, 0xFFFFFFFF, true);

        graphics.drawString(minecraft.font,
                String.format("Player: X: %.0f, Z: %.0f", minecraft.player.getX(), minecraft.player.getZ()),
                10, 46, 0xFFFFFFFF, true);

        graphics.drawString(minecraft.font,
                "Drag: Pan | Scroll: Zoom | ESC: Close",
                10, this.height - 20, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = true;
            lastMouseX = mouseX;
            lastMouseZ = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging) {
            // Pan in tile space
            double tileScreenSize = TileManager.TILE_SIZE * zoom;
            double deltaX = (mouseX - lastMouseX) / tileScreenSize;
            double deltaZ = (mouseY - lastMouseZ) / tileScreenSize;

            cameraTileX -= deltaX;
            cameraTileZ -= deltaZ;

            lastMouseX = mouseX;
            lastMouseZ = mouseY;

            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double zoomChange = scrollY * 0.1;
        zoom = Math.max(0.25, Math.min(4.0, zoom + zoomChange));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        for (TileTextureData data : tileTextures.values()) {
            try {
                data.texture.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        tileTextures.clear();
        super.removed();
    }

    private static class TileTextureData {
        final ResourceLocation location;
        final DynamicTexture texture;
        final int tileX;
        final int tileZ;

        TileTextureData(ResourceLocation location, DynamicTexture texture, int tileX, int tileZ) {
            this.location = location;
            this.texture = texture;
            this.tileX = tileX;
            this.tileZ = tileZ;
        }
    }
}