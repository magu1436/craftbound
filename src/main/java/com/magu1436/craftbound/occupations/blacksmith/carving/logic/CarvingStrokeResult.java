package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

import java.util.List;

public record CarvingStrokeResult(List<ChangedCell> changedCells, double actualRemovedUnits) {
    public CarvingStrokeResult { changedCells = List.copyOf(changedCells); }
    public record ChangedCell(int x, int y, double value) {}
}
