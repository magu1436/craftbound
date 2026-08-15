package com.magu1436.craftbound.occupations.blacksmith.carving.evaluation;

import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;

public interface CarvingBreakEvaluator { boolean shouldBreak(CarvingGrid actual, CarvingGrid ideal, double threshold); }
