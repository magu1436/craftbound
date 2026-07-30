package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 遠征歩行による移動時の食料消耗軽減を適用する。
 */
public final class ExpeditionEnduranceService {

    private ExpeditionEnduranceService() {
    }

    public static float reduceMovementExhaustion(
        Player player,
        float exhaustion
    ) {
        if (!isEligible(player)) {
            return exhaustion;
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.EXPEDITION_ENDURANCE.get()
        );
        return MovementExhaustionReductionCalculator.reduce(
            exhaustion,
            reduction
        );
    }

    private static boolean isEligible(Player player) {
        return player instanceof ServerPlayer serverPlayer
            && !(serverPlayer instanceof FakePlayer)
            && serverPlayer.gameMode.getGameModeForPlayer()
                == GameType.SURVIVAL;
    }
}
