package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 凍結ゲージの蓄積を確率で取り消す処理を提供する。
 */
public final class ColdAdaptationService {

    private ColdAdaptationService() {
    }

    public static int reduceFrozenTicks(
        LivingEntity entity,
        int frozenTicks
    ) {
        if (!(entity instanceof ServerPlayer player)
            || player instanceof FakePlayer
            || player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL
            || frozenTicks != player.getTicksFrozen() + 1) {
            return frozenTicks;
        }

        double reduction = player.getAttributeValue(
            CraftboundAttributes.COLD_ADAPTATION.get()
        );
        return player.getRandom().nextDouble() < reduction
            ? player.getTicksFrozen()
            : frozenTicks;
    }
}
