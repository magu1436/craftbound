package com.magu1436.craftbound.event;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record MovementPenaltyReductionRule(
    Supplier<? extends Attribute> attribute,
    TagKey<Item> targetItems,
    TagKey<Item> excludedItems,
    Predicate<ItemStack> defaultMatcher,
    double reductionRatePerPoint
) {

    public MovementPenaltyReductionRule {
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(targetItems, "targetItems must not be null");
        Objects.requireNonNull(excludedItems, "excludedItems must not be null");
        Objects.requireNonNull(defaultMatcher, "defaultMatcher must not be null");

        if (
            !Double.isFinite(reductionRatePerPoint)
                || reductionRatePerPoint < 0.0D
        ) {
            throw new IllegalArgumentException(
                "reductionRatePerPoint must be a finite non-negative value"
            );
        }
    }

    public boolean matches(ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack must not be null");

        if (itemStack.is(excludedItems)) {
            return false;
        }
        if (itemStack.is(targetItems)) {
            return true;
        }
        return defaultMatcher.test(itemStack);
    }
}
