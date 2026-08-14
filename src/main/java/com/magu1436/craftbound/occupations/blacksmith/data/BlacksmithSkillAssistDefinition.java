package com.magu1436.craftbound.occupations.blacksmith.data;

import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record BlacksmithSkillAssistDefinition(
    int schemaVersion,
    Gauge forgingGauge,
    List<PrecisionScaleLevel> precisionScale,
    int forceReadingEnabledAtLevel,
    boolean showCurrentValue,
    int strikeReferenceEnabledAtLevel,
    boolean strikeReferenceEnabled,
    List<SmithingInstinctLevel> smithingInstinct,
    InstinctAudio instinctAudio
) {
    public BlacksmithSkillAssistDefinition {
        precisionScale = List.copyOf(precisionScale);
        smithingInstinct = List.copyOf(smithingInstinct);
    }

    public record Gauge(
        double min,
        double max,
        int cycleTicks,
        double initialValue,
        GaugeDirection initialDirection,
        int baseMarkInterval
    ) {}

    public record PrecisionScaleLevel(int level, int markInterval) {}
    public record SmithingInstinctLevel(int level, int forgingRemainingHits) {}
    public record InstinctAudio(
        ResourceLocation soundId,
        float volume,
        int cooldownTicks,
        float cautionPitch,
        float dangerPitch,
        float criticalPitch
    ) {}
}
