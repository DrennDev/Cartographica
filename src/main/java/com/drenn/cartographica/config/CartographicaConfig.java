package com.drenn.cartographica.config;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.ModConfigSpec;

public class CartographicaConfig {

    public enum PlayerIndicatorType {
        SIMPLE_TRIANGLE,
        ARROW_SHAPE,
        TEXTURE
    }

    public enum MinimapShape {
        SQUARE,
        CIRCLE
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MINIMAP_SIZE;
    public static final ModConfigSpec.EnumValue<MinimapShape> MINIMAP_SHAPE;
    public static final ModConfigSpec.IntValue MINIMAP_MARGIN;
    public static final ModConfigSpec.BooleanValue SHOW_COORDINATES;
    public static final ModConfigSpec.EnumValue<PlayerIndicatorType> PLAYER_INDICATOR_TYPE;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Minimap Display Settings").push("minimap");
        MINIMAP_SIZE = BUILDER
                .comment("Size of the minimap in pixels (must be between 64 and 512")
                .defineInRange("size", 128, 64, 512);
        MINIMAP_SHAPE = BUILDER
                .comment("Shape of the minimap")
                .defineEnum("shape", MinimapShape.SQUARE);
        MINIMAP_MARGIN = BUILDER
                .comment("Distance from screen edge in pizels")
                .defineInRange("margin", 10, 0, 100);
        SHOW_COORDINATES = BUILDER
                .comment("Display player coordinates below the minimap")
                .define("showCoordinates", false);
        PLAYER_INDICATOR_TYPE = BUILDER
                .comment("Type of player indicator on the minimap")
                .defineEnum("playerIndicator", PlayerIndicatorType.ARROW_SHAPE);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

}
