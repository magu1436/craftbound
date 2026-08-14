package com.magu1436.craftbound.occupations.blacksmith.casting.definition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.*;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;
import com.magu1436.craftbound.occupations.blacksmith.casting.finished.MetalPartItem;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class MetalPartDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final TagKey<Item> CASTING_MOLDS = TagKey.create(
        Registries.ITEM, new ResourceLocation(Craftbound.MODID, "casting_molds"));
    public static final MetalPartDefinitions INSTANCE = new MetalPartDefinitions();

    private volatile Map<ResourceLocation, MetalPartDefinition> definitionsById = Map.of();
    private volatile Map<ResourceLocation, MetalPartDefinition> definitionsByMold = Map.of();

    private MetalPartDefinitions() { super(GSON, "blacksmith/metal_parts"); }

    public Optional<MetalPartDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitionsById.get(id));
    }

    public Set<ResourceLocation> ids() {
        return definitionsById.keySet();
    }

    public Optional<MetalPartDefinition> resolve(ItemStack mold) {
        if (!isRegisteredMold(mold)) return Optional.empty();
        ResourceLocation moldId = ForgeRegistries.ITEMS.getKey(mold.getItem());
        return Optional.ofNullable(definitionsByMold.get(moldId));
    }

    public boolean isRegisteredMold(ItemStack stack) {
        return !stack.isEmpty() && stack.is(CASTING_MOLDS);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> input,
        ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, MetalPartDefinition> byId = new HashMap<>();
        Map<ResourceLocation, MetalPartDefinition> byMold = new HashMap<>();
        Set<ResourceLocation> ambiguousMolds = new HashSet<>();
        input.forEach((fileId, element) -> {
            ResourceLocation definitionId = definitionId(fileId);
            try {
                MetalPartDefinition definition = parse(definitionId, element);
                byId.put(definitionId, definition);
                ResourceLocation moldId = definition.moldItemId();
                if (ambiguousMolds.contains(moldId)
                    || byMold.putIfAbsent(moldId, definition) != null) {
                    ambiguousMolds.add(moldId);
                    byMold.remove(moldId);
                    LOGGER.warn("Duplicate blacksmith metal part mold: {}", moldId);
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Skipping invalid blacksmith metal part definition {}: {}",
                    definitionId, exception.getMessage());
            }
        });
        definitionsById = Map.copyOf(byId);
        definitionsByMold = Map.copyOf(byMold);
        LOGGER.info("Loaded {} blacksmith metal part definitions", byId.size());
    }

    private static ResourceLocation definitionId(ResourceLocation fileId) {
        return new ResourceLocation(fileId.getNamespace(), fileId.getPath());
    }

    static MetalPartDefinition parse(ResourceLocation id, JsonElement element) {
        if (!element.isJsonObject()) throw new JsonParseException("definition must be an object");
        JsonObject json = element.getAsJsonObject();
        require(GsonHelper.getAsInt(json, "schema_version") == 2, "schema_version must be 2");
        int count = GsonHelper.getAsInt(json, "ingredient_count");
        require(count >= 1, "ingredient_count must be positive");
        ResourceLocation mold = registeredItem(json, "mold");
        ResourceLocation roughOutput = registeredItem(json, "rough_output");
        Item roughOutputItem = ForgeRegistries.ITEMS.getValue(roughOutput);
        require(roughOutputItem instanceof RoughMetalPartItem,
            "rough_output must reference a RoughMetalPartItem");
        ResourceLocation output = registeredItem(json, "output");
        Item outputItem = ForgeRegistries.ITEMS.getValue(output);
        require(outputItem instanceof MetalPartItem,
            "output must reference a MetalPartItem");
        JsonObject failure = GsonHelper.getAsJsonObject(json, "failure_lump");
        FailureLumpDefinition failureLump = new FailureLumpDefinition(
            registeredItem(failure, "item"), positiveInt(failure, "count"),
            positiveInt(failure, "units_per_item"));
        JsonObject coolingJson = GsonHelper.getAsJsonObject(json, "cooling");
        long solid = GsonHelper.getAsLong(coolingJson, "surface_solid_ticks");
        long safe = GsonHelper.getAsLong(coolingJson, "safe_ticks");
        int minimumBreak = positiveInt(coolingJson, "minimum_break_on_hit");
        require(solid >= 0 && safe >= solid, "invalid cooling tick range");
        CoolingDefinition cooling = new CoolingDefinition(solid, safe, minimumBreak,
            id(coolingJson, "evaluator"));
        JsonObject forgingJson = GsonHelper.getAsJsonObject(json, "forging");
        double min = GsonHelper.getAsDouble(forgingJson, "strength_min");
        double max = GsonHelper.getAsDouble(forgingJson, "strength_max");
        double strengthPenalty = nonNegative(forgingJson, "strength_penalty_per_point");
        int idealHits = positiveInt(forgingJson, "ideal_hits");
        double hitPenalty = nonNegative(forgingJson, "hit_count_penalty");
        int breakOnHit = positiveInt(forgingJson, "break_on_hit");
        double strengthWeight = nonNegative(forgingJson, "strength_weight");
        double hitWeight = nonNegative(forgingJson, "hit_count_weight");
        require(min <= max, "strength_min must not exceed strength_max");
        require(breakOnHit >= minimumBreak, "break_on_hit is below minimum_break_on_hit");
        require(strengthWeight + hitWeight > 0, "forging weights must have a positive sum");
        ForgingDefinition forging = new ForgingDefinition(min, max, strengthPenalty,
            idealHits, hitPenalty, breakOnHit, strengthWeight, hitWeight,
            id(forgingJson, "evaluator"));
        JsonObject qualityJson = GsonHelper.getAsJsonObject(json, "part_quality");
        double heatingWeight = nonNegative(qualityJson, "heating_weight");
        double forgingWeight = nonNegative(qualityJson, "forging_weight");
        require(heatingWeight + forgingWeight > 0, "part quality weights must have a positive sum");
        PartQualityDefinition quality = new PartQualityDefinition(heatingWeight,
            forgingWeight, id(qualityJson, "evaluator"));
        return new MetalPartDefinition(id, count, mold, roughOutput, output, failureLump,
            cooling, forging, quality);
    }

    private static int positiveInt(JsonObject json, String field) {
        int value = GsonHelper.getAsInt(json, field);
        require(value >= 1, field + " must be positive");
        return value;
    }
    private static double nonNegative(JsonObject json, String field) {
        double value = GsonHelper.getAsDouble(json, field);
        require(Double.isFinite(value) && value >= 0, field + " must be non-negative");
        return value;
    }
    private static ResourceLocation registeredItem(JsonObject json, String field) {
        ResourceLocation value = id(json, field);
        require(ForgeRegistries.ITEMS.containsKey(value), "unknown item `" + value + "`");
        return value;
    }
    private static ResourceLocation id(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        ResourceLocation id = ResourceLocation.tryParse(value);
        require(id != null && value.contains(":"), field + " must be a namespaced id");
        return id;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new JsonParseException(message);
    }
}
