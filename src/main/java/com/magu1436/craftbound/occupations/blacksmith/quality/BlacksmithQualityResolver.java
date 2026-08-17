package com.magu1436.craftbound.occupations.blacksmith.quality;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityPerformanceDefinitions;
import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;

public final class BlacksmithQualityResolver {
    private BlacksmithQualityResolver() {}

    public static OptionalInt resolveForPerformance(ItemStack stack) {
        return resolveForPerformance(
            QualityTargetRegistry.contains(stack),
            readExplicitQuality(stack),
            BlacksmithQualityPerformanceDefinitions.defaultQuality()
        );
    }

    public static OptionalInt resolveForTooltip(ItemStack stack) {
        if (stack.isEmpty()) return OptionalInt.empty();

        return resolveForTooltip(
            QualityTargetRegistry.contains(stack),
            readExplicitQuality(stack),
            BlacksmithQualityPerformanceDefinitions.defaultQuality()
        );
    }

    static OptionalInt resolveForPerformance(
        boolean target,
        OptionalInt explicitQuality,
        int defaultQuality
    ) {
        if (!target) return OptionalInt.empty();
        return explicitQuality.isPresent() ? explicitQuality : OptionalInt.of(defaultQuality);
    }

    static OptionalInt resolveForTooltip(
        boolean target,
        OptionalInt explicitQuality,
        int defaultQuality
    ) {
        if (explicitQuality.isPresent()) return explicitQuality;
        return target ? OptionalInt.of(defaultQuality) : OptionalInt.empty();
    }

    private static OptionalInt readExplicitQuality(ItemStack stack) {
        return QualityStateService.read(stack)
            .map(state -> OptionalInt.of(state.quality()))
            .orElseGet(OptionalInt::empty);
    }
}
