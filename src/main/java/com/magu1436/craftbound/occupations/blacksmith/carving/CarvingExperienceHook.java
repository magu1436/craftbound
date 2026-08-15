package com.magu1436.craftbound.occupations.blacksmith.carving;

import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceNotifier;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithExperienceService;
import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithProcessExperienceSource.AwardResult;

import net.minecraft.server.level.ServerPlayer;

public final class CarvingExperienceHook {
    private CarvingExperienceHook() {}
    public static void onResult(ServerPlayer player, CarvingExperienceResult result) {
        AwardResult awardResult =
            BlacksmithExperienceService.processCarvingResult(
                player,
                result.operatorId(),
                result.shapeMatchPercentage(),
                result.success(),
                result.permanentMaterialLoss()
            );
        BlacksmithExperienceNotifier.notifyGranted(player, awardResult);
    }
}
