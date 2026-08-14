package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import java.util.List;

public record CastingMaskGeometry(
    List<MaskSpan> rimSpans,
    List<MaskSpan> cavitySpans
) {
    public CastingMaskGeometry {
        rimSpans = List.copyOf(rimSpans);
        cavitySpans = List.copyOf(cavitySpans);
    }
}
