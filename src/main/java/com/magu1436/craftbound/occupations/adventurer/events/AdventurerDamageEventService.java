package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.List;
import java.util.Objects;

import com.magu1436.craftbound.event.DamageReductionRule;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class AdventurerDamageEventService {

    private static final List<DamageReductionRule> RULES = List.of(
            new DamageReductionRule(
                    DamageTypeTags.IS_EXPLOSION,
                    CraftboundAttributes.EXPLOSION_DAMAGE_REDUCTION
            )
    );
    
    public static void reductionDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        double reductionRate = RULES.stream()
                .filter(rule ->
                        event.getSource().is(Objects.requireNonNull(rule.damageTypeTag()))
                )
                .mapToDouble(rule ->
                        player.getAttributeValue(
                                Objects.requireNonNull(rule.attribute().get())
                        )
                )
                .max()
                .orElse(0.0D);

        reductionRate = Mth.clamp(
                reductionRate,
                0.0D,
                1.0D
        );

        event.setAmount(
                (float) (
                        event.getAmount()
                                * (1.0D - reductionRate)
                )
        );
    }
}
