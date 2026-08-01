package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 登攀術による垂直移動速度の補正値を提供する。
 */
public final class ClimbingService {

    private ClimbingService() {
    }

    public static double getSpeedBonus(Player player) {
        if (!canApply(player)) {
            return 0.0D;
        }

        return Mth.clamp(
            player.getAttributeValue(CraftboundAttributes.CLIMBING.get()),
            0.0D,
            1.0D
        );
    }

    private static boolean canApply(Player player) {
        if (player instanceof FakePlayer
            || player instanceof ServerPlayer serverPlayer
                && serverPlayer.gameMode.getGameModeForPlayer()
                    != GameType.SURVIVAL
            || player.isCreative()
            || player.isSpectator()
            || player.isPassenger()
            || player.isFallFlying()
            || player.isSwimming()
            || player.getAbilities().flying
            || !player.onClimbable()) {
            return false;
        }

        return player.getLastClimbablePos()
            .map(pos -> !player.level().getBlockState(pos)
                .is(Blocks.SCAFFOLDING))
            .orElse(false);
    }
}
