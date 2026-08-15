package com.magu1436.craftbound.occupations.blacksmith.carving.evaluation;

import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGridResampler;

public final class RemainingRatioBreakEvaluator implements CarvingBreakEvaluator {
    @Override public boolean shouldBreak(CarvingGrid actual, CarvingGrid ideal, double threshold) {
        CarvingGrid comparable = actual.size() == ideal.size()
            ? actual : CarvingGridResampler.areaWeighted(actual, ideal.size());
        double remaining = 0.0D;
        double idealUnits = 0.0D;
        for (int y = 0; y < ideal.size(); y++) for (int x = 0; x < ideal.size(); x++) {
            double idealValue = ideal.get(x, y);
            remaining += Math.min(comparable.get(x, y), idealValue);
            idealUnits += idealValue;
        }
        return idealUnits == 0.0D || remaining / idealUnits <= threshold;
    }
}
