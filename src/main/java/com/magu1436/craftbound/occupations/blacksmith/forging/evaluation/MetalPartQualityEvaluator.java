package com.magu1436.craftbound.occupations.blacksmith.forging.evaluation;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.PartQualityDefinition;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;

public final class MetalPartQualityEvaluator {
    public static final ResourceLocation WEIGHTED_METAL_PART_ID =
        CraftboundUtilities.createResourceLocation("weighted_metal_part");

    private MetalPartQualityEvaluator() {}

    public static OptionalInt evaluate(
        int heatingScore,
        int forgingScore,
        PartQualityDefinition definition
    ) {
        if (definition == null
            || !WEIGHTED_METAL_PART_ID.equals(definition.evaluatorId())
            || heatingScore < 0 || heatingScore > 100
            || forgingScore < 0 || forgingScore > 100
            || !Double.isFinite(definition.heatingWeight())
            || definition.heatingWeight() < 0.0D
            || !Double.isFinite(definition.forgingWeight())
            || definition.forgingWeight() < 0.0D) {
            return OptionalInt.empty();
        }
        long rounded = Math.round(
            heatingScore * definition.heatingWeight()
                + forgingScore * definition.forgingWeight()
        );
        return OptionalInt.of((int) Math.max(0L, Math.min(100L, rounded)));
    }
}
