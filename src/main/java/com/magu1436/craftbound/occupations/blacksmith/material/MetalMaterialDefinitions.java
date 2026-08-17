package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

public final class MetalMaterialDefinitions
    extends SimpleJsonResourceReloadListener
    implements MetalMaterialResolver {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "blacksmith/metals";
    private static final int SUPPORTED_SCHEMA_VERSION = 2;

    public static final MetalMaterialDefinitions INSTANCE =
        new MetalMaterialDefinitions();

    private volatile List<MetalIngredient> ingredients = List.of();
    private volatile Map<ResourceLocation, MetalDefinition> definitions =
        Map.of();

    private MetalMaterialDefinitions() {
        super(GSON, DIRECTORY);
    }

    @Override
    public Optional<ResolvedMetalMaterial> resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        for (MetalIngredient ingredient : ingredients) {
            if (ingredient.matches(stack)) {
                return Optional.of(new ResolvedMetalMaterial(
                    ingredient.metalId(),
                    ingredient.size()
                ));
            }
        }
        return Optional.empty();
    }

    public Optional<MetalDefinition> get(ResourceLocation metalId) {
        return Optional.ofNullable(definitions.get(metalId));
    }

    public Optional<ResourceLocation> getRepresentativeItemId(ResourceLocation metalId) {
        return get(metalId).map(MetalDefinition::representativeItemId);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> definitions,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        List<MetalIngredient> loadedIngredients = new ArrayList<>();
        Map<ResourceLocation, MetalDefinition> loadedDefinitions =
            new HashMap<>();

        definitions.forEach((definitionId, element) -> {
            try {
                if (isRequiredModMissing(element)) {
                    return;
                }
                ParsedMetal parsed = parse(definitionId, element);
                loadedIngredients.addAll(parsed.ingredients());
                loadedDefinitions.put(definitionId, parsed.definition());
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Skipping invalid blacksmith metal definition {}: {}",
                    definitionId,
                    exception.getMessage()
                );
            }
        });

        ingredients = List.copyOf(loadedIngredients);
        this.definitions = Map.copyOf(loadedDefinitions);
        LOGGER.info(
            "Loaded {} blacksmith metal definitions",
            loadedDefinitions.size()
        );
    }

    private static boolean isRequiredModMissing(JsonElement element) {
        if (!element.isJsonObject()) {
            return false;
        }
        JsonObject json = element.getAsJsonObject();
        if (!json.has("required_mod")) {
            return false;
        }
        return !ModList.get().isLoaded(GsonHelper.getAsString(json, "required_mod"));
    }

    static ParsedMetal parse(
        ResourceLocation definitionId,
        JsonElement element
    ) {
        if (!element.isJsonObject()) {
            throw new JsonParseException("definition must be a JSON object");
        }

        JsonObject json = element.getAsJsonObject();
        int schemaVersion = GsonHelper.getAsInt(json, "schema_version");
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new JsonParseException(
                "unsupported schema_version `" + schemaVersion + "`"
            );
        }

        List<MetalIngredient> ingredients = parseIngredients(
            definitionId,
            GsonHelper.getAsJsonArray(json, "ingredients")
        );
        double lossRatio = parseLossRatio(json);
        MetalDefinition.LumpLossRounding rounding = parseLossRounding(json);
        HeatingSettings heating = parseHeatingSettings(json);
        ResourceLocation representativeItemId = parseRepresentativeItem(
            json,
            ingredients
        );
        return new ParsedMetal(
            ingredients,
            new MetalDefinition(
                definitionId,
                lossRatio,
                rounding,
                heating.castable(),
                heating.curve(),
                heating.danger(),
                heating.destroy(),
                heating.evaluator(),
                parseDisplayColor(json),
                representativeItemId
            )
        );
    }

    private static List<MetalIngredient> parseIngredients(
        ResourceLocation metalId,
        JsonArray ingredients
    ) {
        if (ingredients.isEmpty()) {
            throw new JsonParseException("ingredients cannot be empty");
        }

        List<MetalIngredient> parsed = new ArrayList<>();
        for (JsonElement element : ingredients) {
            if (!element.isJsonObject()) {
                throw new JsonParseException(
                    "ingredients entries must be objects"
                );
            }
            parsed.add(parseIngredient(metalId, element.getAsJsonObject()));
        }
        return List.copyOf(parsed);
    }

    private static MetalIngredient parseIngredient(
        ResourceLocation metalId,
        JsonObject ingredient
    ) {
        boolean hasItem = ingredient.has("item");
        boolean hasTag = ingredient.has("tag");
        if (hasItem == hasTag) {
            throw new JsonParseException(
                "ingredient must contain exactly one of `item` or `tag`"
            );
        }
        int size = GsonHelper.getAsInt(ingredient, "size");
        if (size <= 0) {
            throw new JsonParseException("ingredient.size must be greater than zero");
        }

        if (hasItem) {
            ResourceLocation itemId = parseId(
                GsonHelper.getAsString(ingredient, "item"),
                "ingredient.item"
            );
            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                throw new JsonParseException("unknown item `" + itemId + "`");
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return new MetalIngredient(metalId, item, null, size);
        }

        ResourceLocation tagId = parseId(
            GsonHelper.getAsString(ingredient, "tag"),
            "ingredient.tag"
        );
        return new MetalIngredient(
            metalId,
            null,
            TagKey.create(Registries.ITEM, tagId),
            size
        );
    }

    private static double parseLossRatio(JsonObject json) {
        double lossRatio = GsonHelper.getAsDouble(json, "lump_loss_ratio");
        if (lossRatio < 0.0D || lossRatio > 1.0D) {
            throw new JsonParseException(
                "lump_loss_ratio must be between 0 and 1"
            );
        }
        return lossRatio;
    }

    private static Integer parseDisplayColor(JsonObject json) {
        if (!json.has("display_color")) {
            return null;
        }
        String value = GsonHelper.getAsString(json, "display_color");
        if (!value.matches("#[0-9A-Fa-f]{6}")) {
            throw new JsonParseException("display_color must use #RRGGBB format");
        }
        return Integer.parseInt(value.substring(1), 16);
    }

    private static ResourceLocation parseRepresentativeItem(
        JsonObject json,
        List<MetalIngredient> ingredients
    ) {
        if (json.has("representative_item")) {
            ResourceLocation itemId = parseId(
                GsonHelper.getAsString(json, "representative_item"),
                "representative_item"
            );
            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                throw new JsonParseException(
                    "unknown representative_item `" + itemId + "`"
                );
            }
            return itemId;
        }
        for (MetalIngredient ingredient : ingredients) {
            if (ingredient.item() != null) {
                return ForgeRegistries.ITEMS.getKey(ingredient.item());
            }
        }
        return null;
    }

    private static MetalDefinition.LumpLossRounding parseLossRounding(
        JsonObject json
    ) {
        String rounding = GsonHelper.getAsString(json, "lump_loss_rounding");
        return switch (rounding) {
            case "ceil" -> MetalDefinition.LumpLossRounding.CEIL;
            case "floor" -> MetalDefinition.LumpLossRounding.FLOOR;
            case "round" -> MetalDefinition.LumpLossRounding.ROUND;
            default -> throw new JsonParseException(
                "unsupported lump_loss_rounding `" + rounding + "`"
            );
        };
    }

    private static HeatingSettings parseHeatingSettings(JsonObject json) {
        long castable = GsonHelper.getAsLong(json, "castable_after_ticks");
        long danger = GsonHelper.getAsLong(json, "danger_after_ticks");
        long destroy = GsonHelper.getAsLong(json, "destroy_after_ticks");
        if (!(castable >= 0L && castable < danger && danger < destroy)) {
            throw new JsonParseException(
                "expected castable_after_ticks < danger_after_ticks "
                    + "< destroy_after_ticks"
            );
        }

        ResourceLocation evaluator = parseId(
            GsonHelper.getAsString(json, "heating_evaluator"),
            "heating_evaluator"
        );
        JsonArray curve = GsonHelper.getAsJsonArray(json, "score_curve");
        if (curve.isEmpty()) {
            throw new JsonParseException("score_curve cannot be empty");
        }

        long previousTicks = -1L;
        List<MetalDefinition.HeatingScorePoint> points = new ArrayList<>();
        for (JsonElement pointElement : curve) {
            if (!pointElement.isJsonObject()) {
                throw new JsonParseException(
                    "score_curve entries must be objects"
                );
            }
            JsonObject point = pointElement.getAsJsonObject();
            long ticks = GsonHelper.getAsLong(point, "ticks");
            int score = GsonHelper.getAsInt(point, "score");
            if (ticks <= previousTicks) {
                throw new JsonParseException(
                    "score_curve ticks must be strictly ascending"
                );
            }
            if (score < 0 || score > 100) {
                throw new JsonParseException(
                    "score_curve score must be between 0 and 100"
                );
            }
            previousTicks = ticks;
            points.add(new MetalDefinition.HeatingScorePoint(ticks, score));
        }
        return new HeatingSettings(
            castable,
            List.copyOf(points),
            danger,
            destroy,
            evaluator
        );
    }

    private static ResourceLocation parseId(String value, String field) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) {
            throw new JsonParseException(
                field + " must be a namespaced resource id"
            );
        }
        return id;
    }

    record MetalIngredient(
        ResourceLocation metalId,
        Item item,
        TagKey<Item> tag,
        int size
    ) {
        boolean matches(ItemStack stack) {
            return item != null ? stack.is(item) : stack.is(tag);
        }
    }

    record ParsedMetal(
        List<MetalIngredient> ingredients,
        MetalDefinition definition
    ) {
    }

    private record HeatingSettings(
        long castable,
        List<MetalDefinition.HeatingScorePoint> curve,
        long danger,
        long destroy,
        ResourceLocation evaluator
    ) {
    }
}
