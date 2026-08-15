package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

import java.util.ArrayList;
import java.util.List;

public final class CarvingBrush {
    public List<Cell> cells(int centerX, int centerY, int gridSize, double radius) {
        if (gridSize < 1 || !Double.isFinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException("invalid brush parameters");
        }
        List<Cell> result = new ArrayList<>();
        for (int y = Math.max(0, (int) Math.floor(centerY - radius));
             y <= Math.min(gridSize - 1, (int) Math.ceil(centerY + radius)); y++) {
            for (int x = Math.max(0, (int) Math.floor(centerX - radius));
                 x <= Math.min(gridSize - 1, (int) Math.ceil(centerX + radius)); x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius) result.add(new Cell(x, y));
            }
        }
        return List.copyOf(result);
    }

    public record Cell(int x, int y) {}
}
