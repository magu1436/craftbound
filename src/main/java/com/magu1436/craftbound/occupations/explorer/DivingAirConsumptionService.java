package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;

/**
 * 潜水術による空気消費軽減を適用する。
 */
public final class DivingAirConsumptionService {

    private DivingAirConsumptionService() {
    }

    /**
     * 潜水術の発動時に空気消費量を 0 にする。
     */
    public static void preventAirConsumption(
        ServerPlayer player,
        LivingBreatheEvent event
    ) {
        if (
            event.canBreathe()
                || event.getConsumeAirAmount() <= 0
        ) {
            return;
        }

        double chance = player.getAttributeValue(
            CraftboundAttributes.DIVING.get()
        );
        if (player.getRandom().nextDouble() < chance) {
            event.setConsumeAirAmount(0);
        }
    }
}
