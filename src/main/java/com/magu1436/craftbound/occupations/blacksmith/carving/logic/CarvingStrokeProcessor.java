package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CarvingStrokeProcessor {
    private final CarvingBrush brush;

    public CarvingStrokeProcessor() { this(new CarvingBrush()); }
    public CarvingStrokeProcessor(CarvingBrush brush) { this.brush = brush; }

    public CarvingStrokeResult apply(CarvingGrid grid, CarvingStroke stroke,
                                     double brushRadius, double removePerPass) {
        if (!Double.isFinite(removePerPass) || removePerPass <= 0.0D) {
            throw new IllegalArgumentException("removePerPass must be positive");
        }
        List<CarvingBrush.Cell> path = SupercoverLine.trace(
            stroke.startX(), stroke.startY(), stroke.endX(), stroke.endY());
        Map<Integer, CarvingStrokeResult.ChangedCell> changed = new LinkedHashMap<>();
        double removed = 0.0D;
        for (int index = stroke.excludeStart() ? 1 : 0; index < path.size(); index++) {
            CarvingBrush.Cell point = path.get(index);
            for (CarvingBrush.Cell cell : brush.cells(point.x(), point.y(), grid.size(), brushRadius)) {
                double before = grid.get(cell.x(), cell.y());
                double after = Math.max(0.0D, before - removePerPass);
                if (after < before) {
                    grid.set(cell.x(), cell.y(), after);
                    removed += before - after;
                    int key = cell.y() * grid.size() + cell.x();
                    changed.put(key, new CarvingStrokeResult.ChangedCell(cell.x(), cell.y(), after));
                }
            }
        }
        return new CarvingStrokeResult(List.copyOf(changed.values()), removed);
    }
}
