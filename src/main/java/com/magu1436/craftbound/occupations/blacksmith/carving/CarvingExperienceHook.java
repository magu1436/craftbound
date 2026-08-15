package com.magu1436.craftbound.occupations.blacksmith.carving;

import net.minecraft.server.level.ServerPlayer;

public final class CarvingExperienceHook {
    private CarvingExperienceHook() {}
    public static void onResult(ServerPlayer player, CarvingExperienceResult result) {
        // MVP: no-op. Experience integration is intentionally deferred.
    }
}
