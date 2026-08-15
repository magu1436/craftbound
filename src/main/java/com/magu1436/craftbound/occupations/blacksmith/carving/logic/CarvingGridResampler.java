package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

public final class CarvingGridResampler {
    private CarvingGridResampler() {}

    public static CarvingGrid areaWeighted(CarvingGrid source, int targetSize) {
        if (targetSize < 1) throw new IllegalArgumentException("target size must be positive");
        double[] output = new double[targetSize * targetSize];
        for (int ty = 0; ty < targetSize; ty++) {
            for (int tx = 0; tx < targetSize; tx++) {
                double sum = 0.0D;
                for (int sy = 0; sy < source.size(); sy++) {
                    double overlapY = overlap(ty, targetSize, sy, source.size());
                    if (overlapY == 0.0D) continue;
                    for (int sx = 0; sx < source.size(); sx++) {
                        double overlapX = overlap(tx, targetSize, sx, source.size());
                        sum += source.get(sx, sy) * overlapX * overlapY;
                    }
                }
                output[ty * targetSize + tx] = clamp(sum * targetSize * targetSize);
            }
        }
        return new CarvingGrid(targetSize, output);
    }

    private static double overlap(int a, int aSize, int b, int bSize) {
        return Math.max(0.0D, Math.min((a + 1.0D) / aSize, (b + 1.0D) / bSize)
            - Math.max((double) a / aSize, (double) b / bSize));
    }
    private static double clamp(double value) { return Math.max(0.0D, Math.min(1.0D, value)); }
}
