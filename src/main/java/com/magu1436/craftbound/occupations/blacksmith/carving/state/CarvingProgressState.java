package com.magu1436.craftbound.occupations.blacksmith.carving.state;

import com.magu1436.craftbound.occupations.blacksmith.carving.definition.CarvingDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public record CarvingProgressState(int dataVersion, UUID processId,
    ResourceLocation partDefinitionId, ResourceLocation materialProfileId,
    int gridSize, double brushRadius, CarvingGrid carvingGrid,
    double removedUnitsRemainder, CarvingDefinitionSnapshot definitionSnapshot) {
    public static final int CURRENT_VERSION = 1;
    public CarvingProgressState {
        if (dataVersion != CURRENT_VERSION || processId == null || gridSize != carvingGrid.size()
            || !Double.isFinite(brushRadius) || brushRadius <= 0.0D
            || !Double.isFinite(removedUnitsRemainder) || removedUnitsRemainder < 0.0D) {
            throw new IllegalArgumentException("invalid carving progress state");
        }
        carvingGrid = carvingGrid.copy();
    }
}
