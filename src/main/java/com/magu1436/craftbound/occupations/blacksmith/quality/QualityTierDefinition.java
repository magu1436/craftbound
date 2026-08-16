package com.magu1436.craftbound.occupations.blacksmith.quality;

public record QualityTierDefinition(
    String id,
    int min,
    int max,
    String translationKey
) {
    public boolean contains(int quality) {
        return quality >= min && quality <= max;
    }
}
