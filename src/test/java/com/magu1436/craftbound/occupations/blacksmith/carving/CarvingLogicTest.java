package com.magu1436.craftbound.occupations.blacksmith.carving;

import com.magu1436.craftbound.occupations.blacksmith.carving.evaluation.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CarvingLogicTest {
    @Test void clickAndStationaryHoldRemoveActualUnitsOnly() {
        CarvingGrid grid = new CarvingGrid(16);
        CarvingStrokeProcessor processor = new CarvingStrokeProcessor();
        var first = processor.apply(grid, new CarvingStroke(8, 8, 8, 8, false), .5D, .5D);
        var second = processor.apply(grid, new CarvingStroke(8, 8, 8, 8, false), .5D, .5D);
        var empty = processor.apply(grid, new CarvingStroke(8, 8, 8, 8, false), .5D, .5D);
        assertEquals(.5D, first.actualRemovedUnits(), 1.0E-9);
        assertEquals(.5D, second.actualRemovedUnits(), 1.0E-9);
        assertEquals(0.0D, empty.actualRemovedUnits(), 1.0E-9);
    }
    @Test void dragExcludesOnlySharedStart() {
        CarvingGrid grid = new CarvingGrid(16);
        var result = new CarvingStrokeProcessor().apply(grid,
            new CarvingStroke(2, 2, 5, 2, true), .5D, 1.0D);
        assertEquals(3, result.changedCells().size());
        assertEquals(1.0D, grid.get(2, 2));
        assertEquals(0.0D, grid.get(5, 2));
    }
    @Test void resamplingPreservesUniformGridAndIouRange() {
        CarvingGrid source = new CarvingGrid(16);
        CarvingGrid expanded = CarvingGridResampler.areaWeighted(source, 32);
        assertArrayEquals(new CarvingGrid(32).values(), expanded.values(), 1.0E-9);
        assertEquals(100, new IouCarvingShapeEvaluator().evaluate(source, expanded));
    }
    @Test void remainingRatioBreaksAtInclusiveThreshold() {
        double[] ideal = new double[32 * 32]; ideal[0] = 1.0D; ideal[1] = 1.0D;
        double[] actual = ideal.clone(); actual[0] = 0.0D;
        assertTrue(new RemainingRatioBreakEvaluator().shouldBreak(
            new CarvingGrid(32, actual), new CarvingGrid(32, ideal), .5D));
    }
    @Test void supercoverIncludesBothCellsAtCornerCrossing() {
        var cells = SupercoverLine.trace(0, 0, 1, 1);
        assertTrue(cells.contains(new CarvingBrush.Cell(1, 0)));
        assertTrue(cells.contains(new CarvingBrush.Cell(0, 1)));
        assertTrue(cells.contains(new CarvingBrush.Cell(1, 1)));
    }
}
