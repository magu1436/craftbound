package com.magu1436.craftbound.occupations.foodproducer.farming;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FarmlandFertility {

    public static final int MAXIMUM = 100;
    public static final int BASE_PLANTING_COST = 20;
    public static final int FERTILITY_MANAGEMENT_II_PLANTING_COST = 16;
    public static final int BASE_FERTILIZER_RECOVERY = 40;
    public static final int FERTILITY_MANAGEMENT_I_RECOVERY = 50;

    private FarmlandFertility() {
    }

    public static int get(ServerLevel level, BlockPos position) {
        return FarmlandFertilitySavedData.get(level).get(position);
    }

    public static void set(ServerLevel level, BlockPos position, int fertility) {
        FarmlandFertilitySavedData.get(level).set(position, fertility);
    }

    public static boolean consume(ServerLevel level, BlockPos position, int amount) {
        int current = get(level, position);
        if (amount <= 0 || current < amount) {
            return false;
        }
        set(level, position, current - amount);
        return true;
    }

    public static int recover(ServerLevel level, BlockPos position, int amount) {
        int current = get(level, position);
        int updated = clamp(current + Math.max(0, amount));
        if (updated != current) {
            set(level, position, updated);
        }
        return updated - current;
    }

    static int clamp(int fertility) {
        return Math.max(0, Math.min(MAXIMUM, fertility));
    }
}
