package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceNotifier;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceService;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithProcessExperienceSource.AwardResult;

import net.minecraft.server.level.ServerPlayer;

public final class ForgingExperienceHook {
    private ForgingExperienceHook() {}

    public static void onResult(ServerPlayer player, ForgingExperienceResult result) {
        AwardResult awardResult =
            BlacksmithExperienceService.processForgingResult(
                player,
                result.operatorId(),
                result.materialUnits(),
                result.success(),
                result.permanentMaterialLoss()
            );
        BlacksmithExperienceNotifier.notifyGranted(player, awardResult);
    }
}
