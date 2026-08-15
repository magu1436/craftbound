package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class NonMetalMaterialDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    public static final NonMetalMaterialDefinitions INSTANCE = new NonMetalMaterialDefinitions();
    private volatile Map<ResourceLocation, NonMetalMaterialDefinition> definitions = Map.of();
    private NonMetalMaterialDefinitions() { super(GSON, "blacksmith/non_metal_materials"); }
    public Optional<NonMetalMaterialDefinition> get(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager,
                                   ProfilerFiller profiler) {
        Map<ResourceLocation, NonMetalMaterialDefinition> loaded = new HashMap<>();
        input.forEach((id, element) -> { try { loaded.put(id, parse(id, element)); }
            catch (RuntimeException e) { LOGGER.warn("Skipping invalid non-metal material {}: {}", id, e.getMessage()); }});
        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} non-metal material definitions", loaded.size());
    }
    static NonMetalMaterialDefinition parse(ResourceLocation id, JsonElement element) {
        CarvingJson.require(element.isJsonObject(), "definition must be an object");
        JsonObject json = element.getAsJsonObject();
        CarvingJson.require(GsonHelper.getAsInt(json, "schema_version") == 2, "schema_version must be 2");
        ResourceLocation tool = CarvingJson.id(GsonHelper.getAsJsonObject(json, "tool"), "item");
        CarvingJson.require(ForgeRegistries.ITEMS.containsKey(tool), "unknown tool item `" + tool + "`");
        String interpolation = GsonHelper.getAsString(json, "path_interpolation");
        CarvingJson.require("supercover".equals(interpolation), "path_interpolation must be supercover");
        ResourceLocation texture = json.has("carving_texture") ? CarvingJson.id(json, "carving_texture") : null;
        return new NonMetalMaterialDefinition(id, tool, CarvingJson.positive(json, "remove_per_pass"),
            interpolation, CarvingJson.positive(json, "removed_units_per_durability"), texture);
    }
}
