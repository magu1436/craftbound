package com.magu1436.craftbound.common;

import java.util.function.Supplier;

import com.magu1436.craftbound.common.AttributeReward.Operation;

import net.minecraft.world.entity.ai.attributes.Attribute;

public record AttributeRewardArgs(
    String rewardId,
    Supplier<? extends Attribute> attribute,
    Operation operation
) { }
