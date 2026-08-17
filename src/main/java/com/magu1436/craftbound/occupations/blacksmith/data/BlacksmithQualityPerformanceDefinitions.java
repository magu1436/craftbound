package com.magu1436.craftbound.occupations.blacksmith.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.common.quality.QualityState;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceRule;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class BlacksmithQualityPerformanceDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "blacksmith/quality";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final ResourceLocation PERFORMANCE_ID = CraftboundUtilities.createResourceLocation("performance");
    private static final ResourceLocation LINEAR_MULTIPLIER = CraftboundUtilities.createResourceLocation(
        "linear_multiplier"
    );
    private static final Definition DEFAULT_DEFINITION = createDefaultDefinition();

    public static final BlacksmithQualityPerformanceDefinitions INSTANCE =
        new BlacksmithQualityPerformanceDefinitions();

    private static volatile Definition current = DEFAULT_DEFINITION;

    private BlacksmithQualityPerformanceDefinitions() {
        super(GSON, DIRECTORY);
    }

    public static int defaultQuality() {
        return current.defaultQuality();
    }

    public static QualityPerformanceRule rule(QualityPerformanceType type) {
        QualityPerformanceRule rule = current.modifiers().get(type);
        if (rule == null) throw new IllegalStateException("Missing quality performance rule: " + type);
        return rule;
    }

    public static Definition current() {
        return current;
    }

    public static Definition validatedDefinition(
        int defaultQuality,
        Map<QualityPerformanceType, QualityPerformanceRule> modifiers
    ) {
        if (!QualityState.isValidQuality(defaultQuality)) {
            throw new IllegalArgumentException("default_quality is outside the valid range: " + defaultQuality);
        }
        if (modifiers.size() != QualityPerformanceType.values().length) {
            throw new IllegalArgumentException("quality performance definition must contain every modifier");
        }

        EnumMap<QualityPerformanceType, QualityPerformanceRule> validated =
            new EnumMap<>(QualityPerformanceType.class);
        for (QualityPerformanceType type : QualityPerformanceType.values()) {
            QualityPerformanceRule rule = modifiers.get(type);
            if (rule == null) {
                throw new IllegalArgumentException("missing quality performance modifier: " + type.serializedName());
            }
            validateRule(type, rule);
            validated.put(type, rule);
        }
        return new Definition(defaultQuality, validated);
    }

    public static void replace(Definition definition) {
        current = validatedDefinition(definition.defaultQuality(), definition.modifiers());
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> resources,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        JsonElement element = resources.get(PERFORMANCE_ID);
        if (element == null) {
            LOGGER.error(
                "Missing blacksmith/quality/performance.json; keeping the previous quality performance definition"
            );
            return;
        }

        try {
            current = parse(element);
            LOGGER.info("Loaded blacksmith quality performance definition");
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Invalid blacksmith/quality/performance.json; keeping the previous definition: {}",
                exception.getMessage()
            );
        }
    }

    static Definition parse(JsonElement element) {
        if (!element.isJsonObject()) throw new JsonParseException("root must be an object");

        JsonObject root = element.getAsJsonObject();
        int schemaVersion = GsonHelper.getAsInt(root, "schema_version");
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw new JsonParseException("unsupported schema_version: " + schemaVersion);
        }

        int defaultQuality = GsonHelper.getAsInt(root, "default_quality");
        if (!QualityState.isValidQuality(defaultQuality)) {
            throw new JsonParseException("default_quality is outside the valid range: " + defaultQuality);
        }

        JsonObject modifiersJson = GsonHelper.getAsJsonObject(root, "modifiers");
        for (String modifierName : modifiersJson.keySet()) {
            if (QualityPerformanceType.fromSerializedName(modifierName).isEmpty()) {
                throw new JsonParseException("unknown quality performance modifier: " + modifierName);
            }
        }

        EnumMap<QualityPerformanceType, QualityPerformanceRule> modifiers =
            new EnumMap<>(QualityPerformanceType.class);
        for (QualityPerformanceType type : QualityPerformanceType.values()) {
            JsonObject ruleJson = GsonHelper.getAsJsonObject(modifiersJson, type.serializedName());
            modifiers.put(type, parseRule(type, ruleJson));
        }
        return validatedDefinition(defaultQuality, modifiers);
    }

    private static QualityPerformanceRule parseRule(QualityPerformanceType type, JsonObject json) {
        String evaluatorName = GsonHelper.getAsString(json, "evaluator");
        ResourceLocation evaluator = ResourceLocation.tryParse(evaluatorName);
        if (!LINEAR_MULTIPLIER.equals(evaluator)) {
            throw new JsonParseException("unsupported evaluator for " + type.serializedName() + ": " + evaluatorName);
        }

        double base = GsonHelper.getAsDouble(json, "base");
        double perQuality = GsonHelper.getAsDouble(json, "per_quality");
        if (!Double.isFinite(base) || !Double.isFinite(perQuality)) {
            throw new JsonParseException("non-finite value in modifier: " + type.serializedName());
        }

        double roundTo = json.has("round_to") ? GsonHelper.getAsDouble(json, "round_to") : 0.0D;
        if (!Double.isFinite(roundTo) || (json.has("round_to") && roundTo <= 0.0D)) {
            throw new JsonParseException("round_to must be finite and positive for " + type.serializedName());
        }

        QualityPerformanceRule.Rounding rounding = parseRounding(type, json);
        boolean preserveDamageRatio = GsonHelper.getAsBoolean(json, "preserve_damage_ratio", false);
        validateTypeSpecificOptions(type, roundTo, rounding, preserveDamageRatio);
        return new QualityPerformanceRule(
            evaluator,
            base,
            perQuality,
            roundTo,
            rounding,
            preserveDamageRatio
        );
    }

    private static QualityPerformanceRule.Rounding parseRounding(
        QualityPerformanceType type,
        JsonObject json
    ) {
        if (!json.has("rounding")) return QualityPerformanceRule.Rounding.NONE;
        String rounding = GsonHelper.getAsString(json, "rounding");
        if ("round".equals(rounding)) return QualityPerformanceRule.Rounding.ROUND;
        throw new JsonParseException("unsupported rounding for " + type.serializedName() + ": " + rounding);
    }

    private static void validateTypeSpecificOptions(
        QualityPerformanceType type,
        double roundTo,
        QualityPerformanceRule.Rounding rounding,
        boolean preserveDamageRatio
    ) {
        boolean roundedAttribute = type == QualityPerformanceType.ARMOR
            || type == QualityPerformanceType.ARMOR_TOUGHNESS;
        if (roundedAttribute != (roundTo > 0.0D)) {
            throw new JsonParseException(type.serializedName() + " has invalid round_to configuration");
        }

        boolean maxDurability = type == QualityPerformanceType.MAX_DURABILITY;
        if (maxDurability != (rounding == QualityPerformanceRule.Rounding.ROUND)) {
            throw new JsonParseException(type.serializedName() + " has invalid rounding configuration");
        }
        if (!maxDurability && preserveDamageRatio) {
            throw new JsonParseException(type.serializedName() + " has invalid preserve_damage_ratio configuration");
        }
    }

    private static void validateRule(QualityPerformanceType type, QualityPerformanceRule rule) {
        if (!LINEAR_MULTIPLIER.equals(rule.evaluator())) {
            throw new IllegalArgumentException("unsupported evaluator for " + type.serializedName());
        }
        if (!Double.isFinite(rule.base()) || !Double.isFinite(rule.perQuality())) {
            throw new IllegalArgumentException("non-finite value in modifier: " + type.serializedName());
        }
        if (!Double.isFinite(rule.roundTo()) || rule.roundTo() < 0.0D || rule.rounding() == null) {
            throw new IllegalArgumentException("invalid rounding value in modifier: " + type.serializedName());
        }
        validateTypeSpecificOptions(
            type,
            rule.roundTo(),
            rule.rounding(),
            rule.preserveDamageRatio()
        );
    }

    private static Definition createDefaultDefinition() {
        EnumMap<QualityPerformanceType, QualityPerformanceRule> modifiers =
            new EnumMap<>(QualityPerformanceType.class);
        for (QualityPerformanceType type : QualityPerformanceType.values()) {
            double roundTo = type == QualityPerformanceType.ARMOR
                || type == QualityPerformanceType.ARMOR_TOUGHNESS ? 0.5D : 0.0D;
            boolean maxDurability = type == QualityPerformanceType.MAX_DURABILITY;
            modifiers.put(type, new QualityPerformanceRule(
                LINEAR_MULTIPLIER,
                0.7D,
                0.006D,
                roundTo,
                maxDurability ? QualityPerformanceRule.Rounding.ROUND : QualityPerformanceRule.Rounding.NONE,
                maxDurability
            ));
        }
        return validatedDefinition(30, modifiers);
    }

    public record Definition(
        int defaultQuality,
        Map<QualityPerformanceType, QualityPerformanceRule> modifiers
    ) {
        public Definition {
            modifiers = Map.copyOf(modifiers);
        }
    }
}
