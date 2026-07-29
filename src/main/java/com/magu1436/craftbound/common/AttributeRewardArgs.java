package com.magu1436.craftbound.common;

import java.util.Objects;

import com.magu1436.craftbound.common.AttributeReward.Operation;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.util.NonNullSupplier;

public record AttributeRewardArgs(
    String rewardId,
    NonNullSupplier<? extends Attribute> attribute,
    Operation operation
) {
    public AttributeRewardArgs {
        Objects.requireNonNull(rewardId, "reward id is null");
        Objects.requireNonNull(attribute, "attribute supplier is null");
        Objects.requireNonNull(operation, "operation is null");
    }
}
