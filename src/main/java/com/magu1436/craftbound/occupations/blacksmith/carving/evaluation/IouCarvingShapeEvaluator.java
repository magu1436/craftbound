package com.magu1436.craftbound.occupations.blacksmith.carving.evaluation;

import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGridResampler;

public final class IouCarvingShapeEvaluator implements CarvingShapeEvaluator {
    @Override public int evaluate(CarvingGrid actual, CarvingGrid ideal) {
        CarvingGrid comparable = actual.size() == ideal.size()
            ? actual : CarvingGridResampler.areaWeighted(actual, ideal.size());
        double intersection = 0.0D;
        double union = 0.0D;
        for (int y = 0; y < ideal.size(); y++) for (int x = 0; x < ideal.size(); x++) {
            intersection += Math.min(comparable.get(x, y), ideal.get(x, y));
            union += Math.max(comparable.get(x, y), ideal.get(x, y));
        }
        return union == 0.0D ? 0 : (int) Math.round(intersection / union * 100.0D);
    }
}
