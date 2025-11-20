package com.drenn.cartographica.client;

import com.drenn.cartographica.Cartographica;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TileManager {

    public static final int TILE_SIZE = 512;

    private static File tileDirectory;
    private static final ExecutorService renderExecutor = Executors.newFixedThreadPool(2);
    private static final ConcurrentHashMap<String, BufferedImage> tileCache = new ConcurrentHashMap<>();

    public static void initialize(String worldName, String dimensionName) {
        File gameDir = Minecraft.getInstance().gameDirectory;
        tileDirectory = new File(gameDir, "cartographica/" + worldName + "/" + dimensionName + "/tiles");

        if (!tileDirectory.exists()) {
            tileDirectory.mkdirs();
            Cartographica.LOGGER.info("Created tile directory: {}", tileDirectory.getAbsolutePath());
        } else {
            Cartographica.LOGGER.info("Tile directory exists: {}", tileDirectory.getAbsolutePath());
        }

        tileCache.clear();
    }

    public static void shutdown() {
        renderExecutor.shutdownNow();
        tileCache.clear();
    }

    public static int getTileX(int worldX) {
        return Math.floorDiv(worldX, TILE_SIZE);
    }

    public static int getTileZ(int worldZ) {
        return Math.floorDiv(worldZ, TILE_SIZE);
    }

    public static boolean tileExists(int tileX, int tileZ) {
        if (tileDirectory == null) {
            return false;
        }
        File tileFile = new File(tileDirectory, tileX + "_" + tileZ + ".png");
        return tileFile.exists();
    }

    public static BufferedImage loadTile(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;

        if (tileCache.containsKey(key)) {
            return tileCache.get(key);
        }

        if (tileDirectory == null) {
            return null;
        }

        File tileFile = new File(tileDirectory, tileX + "_" + tileZ + ".png");

        if (!tileFile.exists()) {
            return null;
        }

        try {
            BufferedImage image = ImageIO.read(tileFile);
            tileCache.put(key, image);
            return image;
        } catch (IOException e) {
            Cartographica.LOGGER.error("Failed to load tile {}_{}: {}", tileX, tileZ, e.getMessage());
            return null;
        }
    }

    public static void updateChunkInTile(Level level, LevelChunk chunk) {
        if (!level.isClientSide()) {
            return;
        }

        ChunkPos chunkPos = chunk.getPos();
        int chunkWorldX = chunkPos.x << 4;
        int chunkWorldZ = chunkPos.z << 4;

        int tileX = getTileX(chunkWorldX);
        int tileZ = getTileZ(chunkWorldZ);

        renderExecutor.submit(() -> {
            try {
                updateChunkInTileInternal(level, chunk, tileX, tileZ);
            } catch (Exception e) {
                Cartographica.LOGGER.error("Error updating chunk in tile: {}", e.getMessage());
            }
        });
    }

    private static void updateChunkInTileInternal(Level level, LevelChunk chunk, int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;
        BufferedImage tileImage = tileCache.get(key);

        if (tileImage == null) {
            tileImage = loadTile(tileX, tileZ);
        }

        if (tileImage == null) {
            tileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int z = 0; z < TILE_SIZE; z++) {
                    tileImage.setRGB(x, z, 0x00000000);
                }
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockColors blockColors = minecraft.getBlockColors();
        BlockModelShaper modelShaper = minecraft.getBlockRenderer().getBlockModelShaper();

        ChunkPos chunkPos = chunk.getPos();
        int chunkWorldX = chunkPos.x << 4;
        int chunkWorldZ = chunkPos.z << 4;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int worldX = chunkWorldX + dx;
                int worldZ = chunkWorldZ + dz;

                int tileLocalX = worldX - (tileX * TILE_SIZE);
                int tileLocalZ = worldZ - (tileZ * TILE_SIZE);

                if (tileLocalX < 0 || tileLocalX >= TILE_SIZE ||
                        tileLocalZ < 0 || tileLocalZ >= TILE_SIZE) {
                    continue;
                }

                int worldY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                worldY = Math.max(level.getMinBuildHeight(), Math.min(worldY, level.getMaxBuildHeight()));

                BlockPos pos = new BlockPos(worldX, worldY, worldZ);
                BlockState blockState = level.getBlockState(pos);

                int color = getBlockColor(blockColors, modelShaper, level, pos, blockState);
                tileImage.setRGB(tileLocalX, tileLocalZ, color);
            }
        }

        tileCache.put(key, tileImage);
        saveTile(tileX, tileZ, tileImage);
    }

    public static void generateAndSaveTile(Level level, int tileX, int tileZ) {
        renderExecutor.submit(() -> {
            try {
                generateAndSaveTileInternal(level, tileX, tileZ);
            } catch (Exception e) {
                Cartographica.LOGGER.error("Error generating tile: {}", e.getMessage());
            }
        });
    }

    private static void generateAndSaveTileInternal(Level level, int tileX, int tileZ) {
        BufferedImage tileImage = loadTile(tileX, tileZ);

        if (tileImage == null) {
            tileImage = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < TILE_SIZE; x++) {
                for (int z = 0; z < TILE_SIZE; z++) {
                    tileImage.setRGB(x, z, 0x00000000);
                }
            }
        }

        Minecraft minecraft = Minecraft.getInstance();
        BlockColors blockColors = minecraft.getBlockColors();
        BlockModelShaper modelShaper = minecraft.getBlockRenderer().getBlockModelShaper();

        int startChunkX = (tileX * TILE_SIZE) >> 4;
        int startChunkZ = (tileZ * TILE_SIZE) >> 4;
        int endChunkX = ((tileX * TILE_SIZE) + TILE_SIZE - 1) >> 4;
        int endChunkZ = ((tileZ * TILE_SIZE) + TILE_SIZE - 1) >> 4;

        int chunksUpdated = 0;

        for (int chunkX = startChunkX; chunkX <= endChunkX; chunkX++) {
            for (int chunkZ = startChunkZ; chunkZ <= endChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                renderChunkToTile(level, chunk, tileX, tileZ, tileImage, blockColors, modelShaper);
                chunksUpdated++;
            }
        }

        if (chunksUpdated > 0) {
            String key = tileX + "_" + tileZ;
            tileCache.put(key, tileImage);
            saveTile(tileX, tileZ, tileImage);
            Cartographica.LOGGER.info("Generated tile {}_{} with {} chunks", tileX, tileZ, chunksUpdated);
        }
    }

    private static void renderChunkToTile(Level level, LevelChunk chunk, int tileX, int tileZ,
                                          BufferedImage tileImage, BlockColors blockColors,
                                          BlockModelShaper modelShaper) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkWorldX = chunkPos.x << 4;
        int chunkWorldZ = chunkPos.z << 4;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int worldX = chunkWorldX + dx;
                int worldZ = chunkWorldZ + dz;

                int tileLocalX = worldX - (tileX * TILE_SIZE);
                int tileLocalZ = worldZ - (tileZ * TILE_SIZE);

                if (tileLocalX < 0 || tileLocalX >= TILE_SIZE ||
                        tileLocalZ < 0 || tileLocalZ >= TILE_SIZE) {
                    continue;
                }

                int worldY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ) - 1;
                worldY = Math.max(level.getMinBuildHeight(), Math.min(worldY, level.getMaxBuildHeight()));

                BlockPos pos = new BlockPos(worldX, worldY, worldZ);
                BlockState blockState = level.getBlockState(pos);

                int color = getBlockColor(blockColors, modelShaper, level, pos, blockState);
                tileImage.setRGB(tileLocalX, tileLocalZ, color);
            }
        }
    }

    /**
     * Get block color using Minecraft's ACTUAL texture - NO MODIFICATIONS!
     */
    private static int getBlockColor(BlockColors blockColors, BlockModelShaper modelShaper,
                                     Level level, BlockPos pos, BlockState blockState) {
        if (blockState.isAir()) {
            return 0x00000000;
        }

        try {
            // Get texture color
            BakedModel model = modelShaper.getBlockModel(blockState);
            TextureAtlasSprite sprite = model.getParticleIcon();
            int textureColor = sampleSpriteAverage(sprite);

            // Apply biome tint if Minecraft wants it
            int tintColor = blockColors.getColor(blockState, level, pos, 0);
            if (tintColor != -1 && tintColor != 0xFFFFFF) {
                textureColor = multiplyColors(textureColor, tintColor);
            }

            // Brighten to compensate for lack of 3D lighting
            textureColor = brightenColor(textureColor, 1.15f);

            return 0xFF000000 | textureColor;

        } catch (Exception e) {
            return 0xFF888888;
        }
    }

    /**
     * Sample texture color - just read the pixels!
     */
    private static int sampleSpriteAverage(TextureAtlasSprite sprite) {
        try {
            int width = sprite.contents().width();
            int height = sprite.contents().height();

            if (width == 0 || height == 0) {
                return 0x888888;
            }

            long totalR = 0;
            long totalG = 0;
            long totalB = 0;
            int validSamples = 0;

            // Sample a 4x4 grid across the texture
            for (int sy = 0; sy < 4; sy++) {
                for (int sx = 0; sx < 4; sx++) {
                    // Calculate sample position
                    int x = (width * sx) / 4 + width / 8;
                    int y = (height * sy) / 4 + height / 8;

                    // Clamp to bounds
                    if (x >= width) x = width - 1;
                    if (y >= height) y = height - 1;

                    // Read pixel color (ABGR format)
                    int rgba = sprite.getPixelRGBA(0, x, y);

                    // Extract color channels
                    int a = (rgba >> 24) & 0xFF;  // Alpha
                    int b = (rgba >> 16) & 0xFF;  // Blue
                    int g = (rgba >> 8) & 0xFF;   // Green
                    int r = rgba & 0xFF;          // Red

                    // Skip transparent pixels (they're not part of the block!)
                    if (a < 128) {
                        continue;
                    }

                    // Add to total
                    totalR += r;
                    totalG += g;
                    totalB += b;
                    validSamples++;
                }
            }

            // If no valid samples, return gray
            if (validSamples == 0) {
                return 0x888888;
            }

            // Calculate average color
            int avgR = (int) (totalR / validSamples);
            int avgG = (int) (totalG / validSamples);
            int avgB = (int) (totalB / validSamples);

            return (avgR << 16) | (avgG << 8) | avgB;

        } catch (Exception e) {
            Cartographica.LOGGER.error("Error sampling sprite: {}", e.getMessage());
            return 0x888888;
        }
    }

    private static int brightenColor(int color, float factor) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Apply brightness factor
        r = Math.min(255, (int)(r * factor));
        g = Math.min(255, (int)(g * factor));
        b = Math.min(255, (int)(b * factor));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Standard multiply for biome tinting
     */
    private static int multiplyColors(int textureColor, int tintColor) {
        int tr = (textureColor >> 16) & 0xFF;
        int tg = (textureColor >> 8) & 0xFF;
        int tb = textureColor & 0xFF;

        int br = (tintColor >> 16) & 0xFF;
        int bg = (tintColor >> 8) & 0xFF;
        int bb = tintColor & 0xFF;

        int r = (tr * br) / 255;
        int g = (tg * bg) / 255;
        int b = (tb * bb) / 255;

        return (r << 16) | (g << 8) | b;
    }

    private static void saveTile(int tileX, int tileZ, BufferedImage image) {
        if (tileDirectory == null) {
            return;
        }

        File tileFile = new File(tileDirectory, tileX + "_" + tileZ + ".png");

        try {
            ImageIO.write(image, "png", tileFile);
        } catch (IOException e) {
            Cartographica.LOGGER.error("Failed to save tile {}_{}: {}", tileX, tileZ, e.getMessage());
        }
    }

    public static void invalidateTile(int tileX, int tileZ) {
        String key = tileX + "_" + tileZ;
        tileCache.remove(key);
    }
}