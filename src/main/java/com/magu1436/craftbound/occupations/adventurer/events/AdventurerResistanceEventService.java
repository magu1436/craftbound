package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.List;
import java.util.Objects;

import com.magu1436.craftbound.event.MobEffectDurationReductionRule;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITagManager;

public final class AdventurerResistanceEventService {

    private static final double MAX_REDUCTION_RATE = 0.4D;

    private static final List<MobEffectDurationReductionRule> RULES = List.of();

    private AdventurerResistanceEventService() {
    }

    public static void reduceDuration(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MobEffectInstance effectInstance = event.getEffectInstance();

        if (
                effectInstance.isInfiniteDuration()
                || effectInstance.getDuration() <= 0
        ) {
            return;
        }

        ITagManager<MobEffect> tagManager = ForgeRegistries.MOB_EFFECTS.tags();

        if (tagManager == null) {
            return;
        }

        double reductionRate = RULES.stream()
                .filter(rule ->
                        tagManager
                                .getTag(rule.mobEffectTag())
                                .contains(effectInstance.getEffect())
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
                MAX_REDUCTION_RATE
        );

        if (reductionRate <= 0.0D) {
            return;
        }

        effectInstance.duration = Math.max(
                1,
                Mth.ceil(
                        effectInstance.getDuration()
                                * (1.0D - reductionRate)
                )
        );
    }
}
