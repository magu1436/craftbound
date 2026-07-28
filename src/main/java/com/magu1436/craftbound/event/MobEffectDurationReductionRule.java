package com.magu1436.craftbound.event;

import java.util.function.Supplier;

import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;

public record MobEffectDurationReductionRule(
    TagKey<MobEffect> mobEffectTag,
    Supplier<? extends Attribute> attribute
) {

}
