package com.magu1436.craftbound.occupations.blacksmith.forging;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ForgingHammerDurabilityHook {
    private ForgingHammerDurabilityHook() {}

    public static void onAcceptedStrike(ServerPlayer player, ItemStack hammer) {
        // MVP: no-op. This is the sole extension point for future durability handling.
    }
}
