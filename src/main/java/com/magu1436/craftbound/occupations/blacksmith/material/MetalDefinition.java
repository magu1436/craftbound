package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.List;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record MetalDefinition(
    ResourceLocation id,
    double lumpLossRatio,
    LumpLossRounding lumpLossRounding,
    long castableAfterTicks,
    List<HeatingScorePoint> scoreCurve,
    long dangerAfterTicks,
    long destroyAfterTicks,
    ResourceLocation heatingEvaluator
) {
    public MetalDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(lumpLossRounding, "lumpLossRounding");
        scoreCurve = List.copyOf(scoreCurve);
        Objects.requireNonNull(heatingEvaluator, "heatingEvaluator");
    }

    public enum LumpLossRounding {
        CEIL,
        FLOOR,
        ROUND
    }

    public record HeatingScorePoint(long ticks, int score) {
    }
}
