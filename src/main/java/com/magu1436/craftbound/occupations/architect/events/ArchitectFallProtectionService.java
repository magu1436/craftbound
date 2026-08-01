package com.magu1436.craftbound.occupations.architect.events;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * 落下耐性 Attribute に応じて通常の落下ダメージ倍率を軽減する。
 */
public final class ArchitectFallProtectionService {

    private static final double MAX_REDUCTION_RATE = 0.95D;

    private ArchitectFallProtectionService() {
    }

    public static void reduceFallDamage(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || player.isCreative()
            || player.isSpectator()) {
            return;
        }

        double reductionRate = Mth.clamp(
            player.getAttributeValue(
                CraftboundAttributes.FALL_DAMAGE_REDUCTION.get()
            ),
            0.0D,
            MAX_REDUCTION_RATE
        );

        event.setDamageMultiplier(
            event.getDamageMultiplier()
                * (float) (1.0D - reductionRate)
        );
    }
}
