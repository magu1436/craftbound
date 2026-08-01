package com.magu1436.craftbound.occupations.architect;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 飛行技術によるエリトラ加速用花火の非消費判定を行う。
 */
public final class FireworkConservationService {

    private FireworkConservationService() {
    }

    public static boolean shouldConserve(
        Player entity,
        ItemStack stack,
        int consumedAmount
    ) {
        if (!(entity instanceof ServerPlayer player)
            || player instanceof FakePlayer
            || consumedAmount != 1
            || !stack.is(Items.FIREWORK_ROCKET)
            || !player.isFallFlying()) {
            return false;
        }

        GameType gameType = player.gameMode.getGameModeForPlayer();
        if (gameType != GameType.SURVIVAL
            && gameType != GameType.ADVENTURE) {
            return false;
        }

        double chance = Mth.clamp(
            player.getAttributeValue(
                CraftboundAttributes.FIREWORK_CONSERVATION_CHANCE.get()
            ),
            0.0D,
            1.0D
        );

        return chance > 0.0D
            && player.getRandom().nextDouble() < chance;
    }
}
