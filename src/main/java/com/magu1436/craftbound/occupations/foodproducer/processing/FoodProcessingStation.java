package com.magu1436.craftbound.occupations.foodproducer.processing;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundBlocks;

import net.minecraft.world.level.block.Block;

public enum FoodProcessingStation {
    COOKING_TABLE("cooking_table", FoodProcessingOperation.CUT),
    HAND_MILL("hand_mill", FoodProcessingOperation.GRIND),
    DRYING_RACK("drying_rack", FoodProcessingOperation.PRESERVE),
    COOKING_POT("cooking_pot", FoodProcessingOperation.HEAT);

    private final String serializedName;
    private final FoodProcessingOperation defaultOperation;

    FoodProcessingStation(String serializedName, FoodProcessingOperation defaultOperation) {
        this.serializedName = serializedName;
        this.defaultOperation = defaultOperation;
    }

    public String serializedName() {
        return serializedName;
    }

    public FoodProcessingOperation defaultOperation() {
        return defaultOperation;
    }

    public boolean usesFuel() {
        return this == COOKING_POT;
    }

    public static FoodProcessingStation fromBlock(Block block) {
        if (block == CraftboundBlocks.HAND_MILL.get()) return HAND_MILL;
        if (block == CraftboundBlocks.DRYING_RACK.get()) return DRYING_RACK;
        if (block == CraftboundBlocks.COOKING_POT.get()) return COOKING_POT;
        return COOKING_TABLE;
    }
}
