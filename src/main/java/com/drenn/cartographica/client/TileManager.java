package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TileManager {

    // Tile configuration
    public static final int TILE_SIZE = 16; // 16x16 blocks per tile

    // Cache directory
    private static Path tileDirectory = null;

    /**
     * Initialize the tile system for the current world
     */
    public static void initialize(String worldName, String dimension) {
        // Get Minecraft's game directory
        File gameDir = Minecraft.getInstance().gameDirectory;

        // Create path: .minecraft/cartographica/[world]/[dimension]/tiles/
        Path cartographicaDir = gameDir.toPath()
                .resolve("cartographica")
                .resolve(worldName)
                .resolve(dimension)
                .resolve("tiles");

        try {
            Files.createDirectories(cartographicaDir);
            tileDirectory = cartographicaDir;
            Cartographica.LOGGER.info("Tile directory initialized: {}", cartographicaDir);
        } catch (IOException e) {
            Cartographica.LOGGER.error("Failed to create tile directory", e);
        }
    }

    /**
     * Get the tile coordinates for a given block position
     */
    public static int getTileX(int blockX) {
        return Math.floorDiv(blockX, TILE_SIZE);
    }

    public static int getTileZ(int blockZ) {
        return Math.floorDiv(blockZ, TILE_SIZE);
    }

    /**
     * Get the file for a specific tile
     */
    public static File getTileFile(int tileX, int tileZ) {
        if (tileDirectory == null) {
            return null;
        }
        return tileDirectory.resolve(String.format("tile_%d_%d.png", tileX, tileZ)).toFile();
    }

    /**
     * Check if a tile exists
     */
    public static boolean tileExists(int tileX, int tileZ) {
        File file = getTileFile(tileX, tileZ);
        return file != null && file.exists();
    }

    /**
     * Load a tile from disk
     */
    public static BufferedImage loadTile(int tileX, int tileZ) {
        File file = getTileFile(tileX, tileZ);
        if (file == null || !file.exists()) {
            return null;
        }

        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            Cartographica.LOGGER.error("Failed to load tile {}_{}", tileX, tileZ, e);
            return null;
        }
    }

    /**
     * Generate a tile by sampling the world
     */
    public static BufferedImage generateTile(Level level, int tileX, int tileZ) {
        BufferedImage image = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);

        // Calculate world coordinates for this tile
        int startX = tileX * TILE_SIZE;
        int startZ = tileZ * TILE_SIZE;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return image;
        }

        int playerY = (int) mc.player.getY();

        // Render each block in the tile
        for (int x = 0; x < TILE_SIZE; x++) {
            for (int z = 0; z < TILE_SIZE; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Get the color for this block
                int color = getBlockColorForTile(level, worldX, worldZ, playerY);

                // Set pixel in image
                image.setRGB(x, z, color);
            }
        }

        return image;
    }

    /**
     * Get block color for tile generation (similar to MinimapRenderer but optimized)
     */
    private static int getBlockColorForTile(Level level, int x, int z, int startY) {
        // Find top block
        int y = getTopBlockY(level, x, z, startY);

        var pos = new BlockPos(x, y, z);
        var state = level.getBlockState(pos);

        if (state.isAir()) {
            return 0x00000000; // Transparent
        }

        Minecraft mc = Minecraft.getInstance();
        var blockColors = mc.getBlockColors();

        // Check if water
        boolean isWater = state.getBlock() instanceof LiquidBlock;

        if (isWater) {
            int waterColor = blockColors.getColor(state, level, pos, 0);
            if (waterColor == -1 || waterColor == 0xFFFFFF) {
                waterColor = 0x3F76E4;
            }

            // Find terrain below
            int terrainY = y;
            for (int checkY = y - 1; checkY >= level.getMinBuildHeight(); checkY--) {
                var checkPos = new BlockPos(x, checkY, z);
                var checkState = level.getBlockState(checkPos);

                if (checkState.isAir() || checkState.getBlock() instanceof LiquidBlock) {
                    continue;
                }

                terrainY = checkY;
                break;
            }

            if (terrainY < y) {
                var terrainPos = new BlockPos(x, terrainY, z);
                var terrainState = level.getBlockState(terrainPos);
                int terrainColor = blockColors.getColor(terrainState, level, terrainPos, 0);

                if (terrainColor == -1 || terrainColor == 0xFFFFFF) {
                    terrainColor = terrainState.getMapColor(level, terrainPos).col;
                }

                return blendColors(waterColor, terrainColor, 0.6f);
            }

            return 0xFF000000 | waterColor;
        }

        // Regular block
        int color = blockColors.getColor(state, level, pos, 0);

        if (color == -1 || color == 0xFFFFFF) {
            color = state.getMapColor(level, pos).col;
        }

        return 0xFF000000 | color;
    }

    private static int getTopBlockY(Level level, int x, int z, int startY) {
        int maxY = Math.min(level.getMaxBuildHeight() - 1, startY + 30);

        for (int y = maxY; y >= level.getMinBuildHeight(); y--) {
            var pos = new BlockPos(x, y, z);
            var state = level.getBlockState(pos);

            if (state.isAir()) {
                continue;
            }

            return y;
        }

        return level.getSeaLevel();
    }

    private static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int)(r1 * ratio + r2 * (1 - ratio));
        int g = (int)(g1 * ratio + g2 * (1 - ratio));
        int b = (int)(b1 * ratio + b2 * (1 - ratio));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Save a tile to disk
     */
    public static void saveTile(BufferedImage image, int tileX, int tileZ) {
        File file = getTileFile(tileX, tileZ);
        if (file == null) {
            return;
        }

        try {
            ImageIO.write(image, "PNG", file);
            Cartographica.LOGGER.info("Saved tile {}_{}", tileX, tileZ);
        } catch (IOException e) {
            Cartographica.LOGGER.error("Failed to save tile {}_{}", tileX, tileZ, e);
        }
    }

    /**
     * Generate and save a tile if it doesn't exist
     */
    public static void ensureTileExists(Level level, int tileX, int tileZ) {
        if (!tileExists(tileX, tileZ)) {
            BufferedImage tile = generateTile(level, tileX, tileZ);
            saveTile(tile, tileX, tileZ);
        }
    }
}