package com.magu1436.craftbound.occupations.blacksmith.experience;

import java.util.Objects;
import java.util.UUID;

import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithProcessExperienceSource.AwardResult;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

import org.slf4j.Logger;

/** 鍛冶工程の経験値計算と付与条件を一元管理する。 */
public final class BlacksmithExperienceService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CASTING_MULTIPLIER = 1;
    private static final int FORGING_MULTIPLIER = 2;
    private static final int CARVING_MULTIPLIER = 3;
    private static final int ASSEMBLY_MULTIPLIER = 1;

    private BlacksmithExperienceService() {
    }

    public static AwardResult processCastingResult(
        ServerPlayer player,
        UUID operatorId,
        int materialUnits,
        boolean success,
        boolean permanentMaterialLoss
    ) {
        return processResult(
            player,
            operatorId,
            materialUnits,
            CASTING_MULTIPLIER,
            success,
            permanentMaterialLoss
        );
    }

    public static AwardResult processForgingResult(
        ServerPlayer player,
        UUID operatorId,
        int materialUnits,
        boolean success,
        boolean permanentMaterialLoss
    ) {
        return processResult(
            player,
            operatorId,
            materialUnits,
            FORGING_MULTIPLIER,
            success,
            permanentMaterialLoss
        );
    }

    public static AwardResult processCarvingResult(
        ServerPlayer player,
        UUID operatorId,
        int materialUnits,
        boolean success,
        boolean permanentMaterialLoss
    ) {
        return processResult(
            player,
            operatorId,
            materialUnits,
            CARVING_MULTIPLIER,
            success,
            permanentMaterialLoss
        );
    }

    public static AwardResult processAssemblyResult(
        ServerPlayer player,
        UUID operatorId,
        int qualityPartCount
    ) {
        return processResult(
            player,
            operatorId,
            qualityPartCount,
            ASSEMBLY_MULTIPLIER,
            true,
            false
        );
    }

    private static AwardResult processResult(
        ServerPlayer player,
        UUID operatorId,
        int materialUnits,
        int multiplier,
        boolean success,
        boolean permanentMaterialLoss
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(operatorId, "operatorId is null");
        if (!isEligiblePlayer(player, operatorId) || materialUnits <= 0) {
            return new AwardResult(true, 0);
        }
        if (success && permanentMaterialLoss) {
            LOGGER.warn(
                "Skipped inconsistent blacksmith experience result for player {}",
                operatorId
            );
            return new AwardResult(true, 0);
        }

        int successExperience = saturatedMultiply(materialUnits, multiplier);
        int requestedExperience;
        if (success) {
            requestedExperience = successExperience;
        } else if (permanentMaterialLoss) {
            requestedExperience = Math.max(1, successExperience / 4);
        } else {
            return new AwardResult(true, 0);
        }

        AwardResult result = BlacksmithProcessExperienceSource.award(
            player,
            requestedExperience
        );
        if (!result.successful()) {
            LOGGER.error(
                "Failed to award {} blacksmith experience to player {}",
                requestedExperience,
                operatorId
            );
        }
        return result;
    }

    private static boolean isEligiblePlayer(
        ServerPlayer player,
        UUID operatorId
    ) {
        return player.getUUID().equals(operatorId)
            && !player.isCreative()
            && !player.isSpectator()
            && !(player instanceof FakePlayer);
    }

    private static int saturatedMultiply(int value, int multiplier) {
        long result = (long) value * multiplier;
        return (int) Math.min(result, Integer.MAX_VALUE);
    }
}
