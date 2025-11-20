package com.drenn.cartographica.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class CartographicaConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Minimap Settings
    public static final ModConfigSpec.BooleanValue MINIMAP_ENABLED;
    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.EnumValue<MinimapShape> MINIMAP_SHAPE;
    public static final ModConfigSpec.EnumValue<MinimapPosition> MINIMAP_POSITION;
    public static final ModConfigSpec.IntValue MINIMAP_MARGIN;
    public static final ModConfigSpec.DoubleValue MINIMAP_ZOOM;
    public static final ModConfigSpec.BooleanValue MINIMAP_ROTATE;

    // Minimap Display
    public static final ModConfigSpec.BooleanValue SHOW_COORDINATES;
    public static final ModConfigSpec.BooleanValue SHOW_TIME;
    public static final ModConfigSpec.BooleanValue SHOW_BIOME;
    public static final ModConfigSpec.BooleanValue SHOW_FPS;

    // Minimap Border
    public static final ModConfigSpec.BooleanValue SHOW_BORDER;
    public static final ModConfigSpec.ConfigValue<Integer> BORDER_COLOR;
    public static final ModConfigSpec.IntValue BORDER_WIDTH;

    // Player Marker
    public static final ModConfigSpec.EnumValue<PlayerMarkerType> PLAYER_MARKER_TYPE;
    public static final ModConfigSpec.ConfigValue<Integer> PLAYER_MARKER_COLOR;
    public static final ModConfigSpec.IntValue PLAYER_MARKER_SIZE;

    static {
        BUILDER.push("Minimap Settings");

        MINIMAP_ENABLED = BUILDER
                .comment("Enable/disable the minimap")
                .define("enabled", true);

        MINIMAP_SIZE = BUILDER
                .comment("Size of the minimap in pixels (default: 128)")
                .defineInRange("size", 128, 64, 512);

        MINIMAP_SHAPE = BUILDER
                .comment("Shape of the minimap: SQUARE or CIRCLE")
                .defineEnum("shape", MinimapShape.SQUARE);

        MINIMAP_POSITION = BUILDER
                .comment("Corner position: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT")
                .defineEnum("position", MinimapPosition.TOP_RIGHT);

        MINIMAP_MARGIN = BUILDER
                .comment("Margin from screen edge in pixels")
                .defineInRange("margin", 10, 0, 100);

        MINIMAP_ZOOM = BUILDER
                .comment("Zoom level (1.0 = default, 2.0 = zoomed in 2x)")
                .defineInRange("zoom", 1.0, 0.5, 4.0);

        MINIMAP_ROTATE = BUILDER
                .comment("Rotate minimap to follow player direction")
                .define("rotate", true);

        BUILDER.pop();

        BUILDER.push("Minimap Display");

        SHOW_COORDINATES = BUILDER
                .comment("Show player coordinates")
                .define("showCoordinates", true);

        SHOW_TIME = BUILDER
                .comment("Show in-game time")
                .define("showTime", true);

        SHOW_BIOME = BUILDER
                .comment("Show current biome")
                .define("showBiome", false);

        SHOW_FPS = BUILDER
                .comment("Show FPS counter")
                .define("showFPS", false);

        BUILDER.pop();

        BUILDER.push("Minimap Border");

        SHOW_BORDER = BUILDER
                .comment("Show border around minimap")
                .define("showBorder", true);

        BORDER_COLOR =  BUILDER
                .comment("Border color (ARGB format, default: white)")
                .define("borderColor", 0xFFFFFFFF);

        BORDER_WIDTH = BUILDER
                .comment("Border width in pixels")
                .defineInRange("borderWidth", 2, 1, 10);

        BUILDER.pop();

        BUILDER.push("Player Marker");

        PLAYER_MARKER_TYPE = BUILDER
                .comment("Player marker style: ARROW, DOT, CROSSHAIR, SURVEYOR")
                .defineEnum("markerType", PlayerMarkerType.ARROW);

        PLAYER_MARKER_COLOR =  BUILDER
                .comment("Player marker color (ARGB format, default: red)")
                .define("markerColor", 0xFFFF0000);

        PLAYER_MARKER_SIZE = BUILDER
                .comment("Player marker size in pixels")
                .defineInRange("markerSize", 8, 4, 20);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Enums for config options
    public enum MinimapShape {
        SQUARE,
        CIRCLE
    }

    public enum MinimapPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public enum PlayerMarkerType {
        ARROW,
        DOT,
        CROSSHAIR,
        SURVEYOR
    }
}