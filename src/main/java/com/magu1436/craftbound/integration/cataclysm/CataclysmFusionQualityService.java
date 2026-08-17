package com.magu1436.craftbound.integration.cataclysm;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.occupations.blacksmith.assembly.FinishedItemQualityCalculator;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;

public final class CataclysmFusionQualityService {
    private CataclysmFusionQualityService() {}

    public static void apply(ItemStack base, ItemStack addition, ItemStack result) {
        if (result.isEmpty()) return;

        List<Integer> qualities = new ArrayList<>(2);
        QualityStateService.read(base).ifPresent(state -> qualities.add(state.quality()));
        QualityStateService.read(addition).ifPresent(state -> qualities.add(state.quality()));

        OptionalInt quality = FinishedItemQualityCalculator.calculate(qualities);
        quality.ifPresent(value -> QualityStateService.setQuality(result, value));
    }
}
