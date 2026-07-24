package com.magu1436.craftbound.event;

import java.util.function.Supplier;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;

public record DamageReductionRule(
    TagKey<DamageType> damageTypeTag,
    Supplier<? extends Attribute> attribute
) {

}
