package com.magu1436.craftbound.occupations.blacksmith.casting;

import net.minecraft.server.level.ServerPlayer;

public final class CastingExperienceHook {
    private CastingExperienceHook() {}

    public static void onResult(ServerPlayer player, CastingExperienceResult result) {
        // MVP: no-op. Experience integration is intentionally deferred.
    }
}
