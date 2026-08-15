package com.magu1436.craftbound.occupations.blacksmith.carving.state;

import com.magu1436.craftbound.occupations.blacksmith.carving.definition.CarvingDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import java.util.Optional;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

public final class CarvingProgressStateCodec {
    private CarvingProgressStateCodec() {}
    public static CompoundTag write(CarvingProgressState state) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", state.dataVersion());
        tag.putUUID("ProcessId", state.processId());
        tag.putString("PartDefinition", state.partDefinitionId().toString());
        tag.putString("MaterialProfile", state.materialProfileId().toString());
        tag.putInt("GridSize", state.gridSize());
        tag.putDouble("BrushRadius", state.brushRadius());
        tag.putDouble("RemovedUnitsRemainder", state.removedUnitsRemainder());
        tag.put("CarvingGrid", writeGrid(state.carvingGrid()));
        tag.put("DefinitionSnapshot", writeSnapshot(state.definitionSnapshot()));
        return tag;
    }
    public static Optional<CarvingProgressState> read(CompoundTag tag) {
        try {
            if (!tag.contains("DataVersion", Tag.TAG_INT) || !tag.hasUUID("ProcessId")
                || !tag.contains("PartDefinition", Tag.TAG_STRING) || !tag.contains("MaterialProfile", Tag.TAG_STRING)
                || !tag.contains("GridSize", Tag.TAG_INT) || !tag.contains("BrushRadius", Tag.TAG_DOUBLE)
                || !tag.contains("RemovedUnitsRemainder", Tag.TAG_DOUBLE)
                || !tag.contains("CarvingGrid", Tag.TAG_LIST)
                || !tag.contains("DefinitionSnapshot", Tag.TAG_COMPOUND)) return Optional.empty();
            CarvingGrid grid = readGrid(tag.getInt("GridSize"), tag.getList("CarvingGrid", Tag.TAG_DOUBLE));
            CarvingDefinitionSnapshot snapshot = readSnapshot(tag.getCompound("DefinitionSnapshot"));
            return Optional.of(new CarvingProgressState(tag.getInt("DataVersion"), tag.getUUID("ProcessId"),
                id(tag, "PartDefinition"), id(tag, "MaterialProfile"), tag.getInt("GridSize"),
                tag.getDouble("BrushRadius"), grid, tag.getDouble("RemovedUnitsRemainder"), snapshot));
        } catch (RuntimeException exception) { return Optional.empty(); }
    }
    private static ListTag writeGrid(CarvingGrid grid) {
        ListTag values = new ListTag();
        for (double value : grid.values()) values.add(DoubleTag.valueOf(value));
        return values;
    }
    private static CarvingGrid readGrid(int size, ListTag values) {
        if (size < 1 || values.size() != size * size) throw new IllegalArgumentException("invalid grid");
        double[] cells = new double[values.size()];
        for (int i = 0; i < values.size(); i++) cells[i] = values.getDouble(i);
        return new CarvingGrid(size, cells);
    }
    private static CompoundTag writeSnapshot(CarvingDefinitionSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        putId(tag, "Output", snapshot.outputItemId()); tag.putInt("IngredientCount", snapshot.ingredientCount());
        putId(tag, "RequiredTool", snapshot.requiredToolItemId()); tag.putDouble("RemovePerPass", snapshot.removePerPass());
        tag.putString("PathInterpolation", snapshot.pathInterpolation());
        tag.putDouble("RemovedUnitsPerDurability", snapshot.removedUnitsPerDurability());
        tag.putDouble("WarningRetention", snapshot.warningRetention()); putId(tag, "BreakEvaluator", snapshot.breakEvaluatorId());
        tag.putDouble("BreakThreshold", snapshot.breakThreshold()); putId(tag, "ShapeEvaluator", snapshot.shapeEvaluatorId());
        tag.putInt("IdealShapeSize", snapshot.idealShape().size()); tag.put("IdealShape", writeGrid(snapshot.idealShape()));
        return tag;
    }
    private static CarvingDefinitionSnapshot readSnapshot(CompoundTag tag) {
        String[] doubles = {"RemovePerPass", "RemovedUnitsPerDurability", "WarningRetention", "BreakThreshold"};
        for (String key : doubles) if (!tag.contains(key, Tag.TAG_DOUBLE)) throw new IllegalArgumentException("missing " + key);
        if (!tag.contains("IngredientCount", Tag.TAG_INT) || !tag.contains("PathInterpolation", Tag.TAG_STRING)
            || !tag.contains("IdealShapeSize", Tag.TAG_INT) || !tag.contains("IdealShape", Tag.TAG_LIST))
            throw new IllegalArgumentException("invalid snapshot");
        return new CarvingDefinitionSnapshot(id(tag, "Output"), tag.getInt("IngredientCount"), id(tag, "RequiredTool"),
            tag.getDouble("RemovePerPass"), tag.getString("PathInterpolation"), tag.getDouble("RemovedUnitsPerDurability"),
            tag.getDouble("WarningRetention"), id(tag, "BreakEvaluator"), tag.getDouble("BreakThreshold"),
            id(tag, "ShapeEvaluator"), readGrid(tag.getInt("IdealShapeSize"), tag.getList("IdealShape", Tag.TAG_DOUBLE)));
    }
    private static void putId(CompoundTag tag, String key, ResourceLocation id) { tag.putString(key, id.toString()); }
    private static ResourceLocation id(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("missing " + key);
        String value = tag.getString(key); ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) throw new IllegalArgumentException("invalid " + key);
        return id;
    }
}
