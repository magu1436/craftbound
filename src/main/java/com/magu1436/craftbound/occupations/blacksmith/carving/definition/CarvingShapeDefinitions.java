package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import com.google.gson.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.CarvingGrid;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class CarvingShapeDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    public static final CarvingShapeDefinitions INSTANCE = new CarvingShapeDefinitions();
    private volatile Map<ResourceLocation, CarvingShapeDefinition> definitions = Map.of();
    private CarvingShapeDefinitions() { super(GSON, "blacksmith/shapes"); }
    public Optional<CarvingShapeDefinition> get(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }
    @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager,
                                   ProfilerFiller profiler) {
        Map<ResourceLocation, CarvingShapeDefinition> loaded = new HashMap<>();
        input.forEach((id, element) -> { try { loaded.put(id, parse(id, element)); }
            catch (RuntimeException e) { LOGGER.warn("Skipping invalid carving shape {}: {}", id, e.getMessage()); }});
        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} carving shape definitions", loaded.size());
    }
    static CarvingShapeDefinition parse(ResourceLocation id, JsonElement element) {
        CarvingJson.require(element.isJsonObject(), "definition must be an object");
        JsonObject json = element.getAsJsonObject();
        CarvingJson.require(GsonHelper.getAsInt(json, "schema_version") == 1, "schema_version must be 1");
        int size = GsonHelper.getAsInt(json, "grid_size");
        CarvingJson.require(size == 32, "grid_size must match maximum size 32");
        CarvingJson.require("binary_rows".equals(GsonHelper.getAsString(json, "encoding")), "unsupported encoding");
        JsonArray rows = GsonHelper.getAsJsonArray(json, "rows");
        CarvingJson.require(rows.size() == size, "rows count must match grid_size");
        double[] mask = new double[size * size];
        for (int y = 0; y < size; y++) {
            String row = rows.get(y).getAsString();
            CarvingJson.require(row.length() == size, "row length must match grid_size");
            for (int x = 0; x < size; x++) {
                char value = row.charAt(x);
                CarvingJson.require(value == '#' || value == '.', "rows may contain only # and .");
                mask[y * size + x] = value == '#' ? 1.0D : 0.0D;
            }
        }
        return new CarvingShapeDefinition(id, new CarvingGrid(size, mask));
    }
}
