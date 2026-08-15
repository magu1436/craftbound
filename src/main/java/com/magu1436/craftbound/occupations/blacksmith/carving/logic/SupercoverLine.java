package com.magu1436.craftbound.occupations.blacksmith.carving.logic;

import java.util.ArrayList;
import java.util.List;

public final class SupercoverLine {
    private SupercoverLine() {}

    public static List<CarvingBrush.Cell> trace(int x0, int y0, int x1, int y1) {
        List<CarvingBrush.Cell> cells = new ArrayList<>();
        int dx = x1 - x0;
        int dy = y1 - y0;
        int nx = Math.abs(dx);
        int ny = Math.abs(dy);
        int signX = Integer.compare(dx, 0);
        int signY = Integer.compare(dy, 0);
        int x = x0;
        int y = y0;
        cells.add(new CarvingBrush.Cell(x, y));
        for (int ix = 0, iy = 0; ix < nx || iy < ny;) {
            long decision = (1L + 2L * ix) * ny - (1L + 2L * iy) * nx;
            if (decision == 0) {
                x += signX; y += signY; ix++; iy++;
            } else if (decision < 0) {
                x += signX; ix++;
            } else {
                y += signY; iy++;
            }
            cells.add(new CarvingBrush.Cell(x, y));
        }
        return List.copyOf(cells);
    }
}
