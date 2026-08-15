package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

import java.util.Arrays;

public final class CarvingGrid {
    private final int size;
    private final double[] cells;

    public CarvingGrid(int size) {
        this(size, filled(size));
    }

    public CarvingGrid(int size, double[] cells) {
        if (size < 1 || cells.length != size * size) {
            throw new IllegalArgumentException("invalid carving grid dimensions");
        }
        this.size = size;
        this.cells = cells.clone();
        for (double cell : this.cells) {
            if (!Double.isFinite(cell) || cell < 0.0D || cell > 1.0D) {
                throw new IllegalArgumentException("carving grid values must be between 0 and 1");
            }
        }
    }

    private static double[] filled(int size) {
        if (size < 1) throw new IllegalArgumentException("grid size must be positive");
        double[] values = new double[size * size];
        Arrays.fill(values, 1.0D);
        return values;
    }

    public int size() { return size; }
    public double get(int x, int y) { check(x, y); return cells[y * size + x]; }
    public void set(int x, int y, double value) {
        check(x, y);
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("cell value must be between 0 and 1");
        }
        cells[y * size + x] = value;
    }
    public double[] values() { return cells.clone(); }
    public CarvingGrid copy() { return new CarvingGrid(size, cells); }

    private void check(int x, int y) {
        if (x < 0 || x >= size || y < 0 || y >= size) {
            throw new IndexOutOfBoundsException("cell outside carving grid");
        }
    }
}
