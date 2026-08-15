package com.magu1436.craftbound.occupations.blacksmith.casting;

import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceNotifier;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceService;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithProcessExperienceSource.AwardResult;

import net.minecraft.server.level.ServerPlayer;

public final class CastingExperienceHook {
    private CastingExperienceHook() {}

    public static void onResult(ServerPlayer player, CastingExperienceResult result) {
        AwardResult awardResult =
            BlacksmithExperienceService.processCastingResult(
                player,
                result.operatorId(),
                result.materialUnits(),
                result.success(),
                result.permanentMaterialLoss()
            );
        BlacksmithExperienceNotifier.notifyGranted(player, awardResult);
    }
}
