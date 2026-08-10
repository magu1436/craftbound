package com.magu1436.craftbound.occupations.blacksmith.forging;

import net.minecraft.server.level.ServerPlayer;

public final class ForgingExperienceHook {
    private ForgingExperienceHook() {}

    public static void onResult(ServerPlayer player, ForgingExperienceResult result) {
        // MVP: no-op. Experience integration is intentionally deferred.
    }
}
