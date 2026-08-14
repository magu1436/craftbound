package com.magu1436.craftbound.occupations.blacksmith.client.casting;

public record MaskSpan(
    int row,
    int startColumn,
    int endColumnExclusive
) {
    public MaskSpan {
        if (row < 0 || startColumn < 0 || endColumnExclusive <= startColumn) {
            throw new IllegalArgumentException("invalid mask span coordinates");
        }
    }
}
