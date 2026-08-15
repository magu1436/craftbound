package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class NonMetalPartDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    public static final NonMetalPartDefinitions INSTANCE = new NonMetalPartDefinitions();
    private volatile Map<ResourceLocation, NonMetalPartDefinition> definitions = Map.of();
    private NonMetalPartDefinitions() { super(GSON, "blacksmith/non_metal_parts"); }
    public Optional<NonMetalPartDefinition> get(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }
    public List<NonMetalPartDefinition> matching(ItemStack stack) {
        return definitions.values().stream().filter(value -> value.matches(stack)).toList();
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager,
                                   ProfilerFiller profiler) {
        Map<ResourceLocation, NonMetalPartDefinition> loaded = new HashMap<>();
        input.forEach((id, element) -> { try { loaded.put(id, parse(id, element)); }
            catch (RuntimeException e) { LOGGER.warn("Skipping invalid non-metal part {}: {}", id, e.getMessage()); }});
        definitions = Map.copyOf(loaded);
        LOGGER.info("Loaded {} non-metal part definitions", loaded.size());
    }
    static NonMetalPartDefinition parse(ResourceLocation id, JsonElement element) {
        CarvingJson.require(element.isJsonObject(), "definition must be an object");
        JsonObject json = element.getAsJsonObject();
        CarvingJson.require(GsonHelper.getAsInt(json, "schema_version") == 2, "schema_version must be 2");
        JsonObject ingredient = GsonHelper.getAsJsonObject(json, "ingredient");
        boolean hasItem = ingredient.has("item"), hasTag = ingredient.has("tag");
        CarvingJson.require(hasItem != hasTag, "ingredient requires exactly one of item or tag");
        Item item = null; TagKey<Item> tag = null;
        if (hasItem) {
            ResourceLocation itemId = CarvingJson.id(ingredient, "item");
            CarvingJson.require(ForgeRegistries.ITEMS.containsKey(itemId), "unknown ingredient item `" + itemId + "`");
            item = ForgeRegistries.ITEMS.getValue(itemId);
        } else tag = TagKey.create(Registries.ITEM, CarvingJson.id(ingredient, "tag"));
        int count = GsonHelper.getAsInt(json, "ingredient_count");
        CarvingJson.require(count >= 1, "ingredient_count must be positive");
        ResourceLocation output = CarvingJson.id(json, "output");
        CarvingJson.require(ForgeRegistries.ITEMS.containsKey(output), "unknown output item `" + output + "`");
        int baseGrid = GsonHelper.getAsInt(json, "base_grid_size");
        CarvingJson.require(baseGrid == 16 || baseGrid == 24 || baseGrid == 32, "base_grid_size must be 16, 24, or 32");
        JsonObject breakCondition = GsonHelper.getAsJsonObject(json, "break_condition");
        return new NonMetalPartDefinition(id, CarvingJson.id(json, "material_profile"), item, tag, count,
            output, CarvingJson.id(json, "shape"), baseGrid,
            CarvingJson.ratio(json, "warning_at_or_below_retention"),
            CarvingJson.id(breakCondition, "type"), CarvingJson.ratio(breakCondition, "threshold"),
            CarvingJson.id(json, "shape_evaluator"));
    }
}
