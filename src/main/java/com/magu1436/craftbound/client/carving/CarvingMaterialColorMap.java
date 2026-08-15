package com.magu1436.craftbound.client.carving;

public record CarvingMaterialColorMap(int size, int[] rgb) {
    public CarvingMaterialColorMap {
        if (size < 1 || rgb.length != size * size) throw new IllegalArgumentException("invalid color map");
        rgb = rgb.clone();
    }
    public int color(int x, int y) { return rgb[y * size + x]; }
    @Override public int[] rgb() { return rgb.clone(); }
}
