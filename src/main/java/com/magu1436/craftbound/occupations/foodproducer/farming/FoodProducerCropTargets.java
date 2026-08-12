package com.magu1436.craftbound.occupations.foodproducer.farming;

import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FoodProducerCropTargets {

    private static final Set<Block> TARGET_BLOCKS = Set.of(
            Blocks.WHEAT,
            Blocks.CARROTS,
            Blocks.POTATOES,
            Blocks.BEETROOTS,
            Blocks.PUMPKIN_STEM,
            Blocks.MELON_STEM,
            Blocks.PUMPKIN,
            Blocks.MELON,
            Blocks.SUGAR_CANE,
            Blocks.COCOA,
            Blocks.SWEET_BERRY_BUSH,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT
    );

    private FoodProducerCropTargets() {
    }

    public static boolean isTarget(BlockState state) {
        return TARGET_BLOCKS.contains(state.getBlock());
    }

    public static boolean consumesFarmlandFertility(BlockState state) {
        return state.is(Blocks.WHEAT)
                || state.is(Blocks.CARROTS)
                || state.is(Blocks.POTATOES)
                || state.is(Blocks.BEETROOTS)
                || state.is(Blocks.PUMPKIN_STEM)
                || state.is(Blocks.MELON_STEM);
    }

    public static boolean isMatureHarvest(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.is(Blocks.COCOA)) {
            return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        }
        return state.is(Blocks.PUMPKIN)
                || state.is(Blocks.MELON);
    }

    public static boolean hasQualityHarvestDrop(BlockState state) {
        return isMatureHarvest(state) || state.is(Blocks.SUGAR_CANE);
    }

    public static Item primaryProduct(BlockState state) {
        if (state.is(Blocks.WHEAT)) return Items.WHEAT;
        if (state.is(Blocks.CARROTS)) return Items.CARROT;
        if (state.is(Blocks.POTATOES)) return Items.POTATO;
        if (state.is(Blocks.BEETROOTS)) return Items.BEETROOT;
        if (state.is(Blocks.PUMPKIN)) return Items.PUMPKIN;
        if (state.is(Blocks.MELON)) return Items.MELON_SLICE;
        if (state.is(Blocks.SUGAR_CANE)) return Items.SUGAR_CANE;
        if (state.is(Blocks.COCOA)) return Items.COCOA_BEANS;
        return Items.AIR;
    }
}
