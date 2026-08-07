package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** 同じ設定を共有する1件以上のバイオーム定義。 */
public record BiomeDiscoveryRule(
    ResourceLocation definitionId,
    Set<ResourceLocation> biomeIds,
    boolean enabled,
    ExperienceSpec experience,
    int dwellTicks,
    Optional<String> translationKey
) {
    public static final int DEFAULT_DWELL_TICKS = 100;
    private static final Set<String> RULE_FIELDS = Set.of(
        "biome", "biomes", "enabled", "experience_tier",
        "xp", "dwell_ticks", "translation_key"
    );
    private static final Set<String> LEGACY_FILE_FIELDS = Set.of(
        "format_version", "biome", "biomes", "enabled",
        "experience_tier", "xp", "dwell_ticks", "translation_key"
    );
    private static final Set<String> GROUPED_FILE_FIELDS = Set.of(
        "format_version", "rules"
    );

    public BiomeDiscoveryRule {
        biomeIds = Set.copyOf(biomeIds);
        if (biomeIds.isEmpty()) {
            throw new IllegalArgumentException("biome ids must not be empty");
        }
    }

    public static List<BiomeDiscoveryRule> parseFile(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        if (!json.has("rules")) {
            return parseLegacyFile(definitionId, json, errors);
        }

        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, GROUPED_FILE_FIELDS, errors
        );
        JsonElement rulesElement = json.get("rules");
        if (rulesElement == null || !rulesElement.isJsonArray()) {
            errors.add(definitionId, "rules must be an array");
            return List.of();
        }
        JsonArray rules = rulesElement.getAsJsonArray();
        if (rules.isEmpty()) {
            errors.add(definitionId, "rules must not be empty");
        }
        if (errors.messageCount() != errorCount) {
            return List.of();
        }

        List<BiomeDiscoveryRule> result = new ArrayList<>();
        for (int index = 0; index < rules.size(); index++) {
            JsonElement element = rules.get(index);
            if (!element.isJsonObject()) {
                errors.add(
                    definitionId,
                    "rules[" + index + "] must be an object"
                );
                continue;
            }
            BiomeDiscoveryRule rule = parseRule(
                definitionId, element.getAsJsonObject(), errors,
                "rules[" + index + "]", false
            );
            if (rule != null) {
                result.add(rule);
            }
        }
        return List.copyOf(result);
    }

    private static List<BiomeDiscoveryRule> parseLegacyFile(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, LEGACY_FILE_FIELDS, errors
        );
        BiomeDiscoveryRule rule = parseRule(
            definitionId, json, errors, "biome rule", true
        );
        return rule == null || errors.messageCount() != errorCount
            ? List.of()
            : List.of(rule);
    }

    private static BiomeDiscoveryRule parseRule(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors,
        String context,
        boolean allowFormatVersion
    ) {
        int errorCount = errors.messageCount();
        validateRuleFields(
            definitionId, json, errors, context, allowFormatVersion
        );
        Set<ResourceLocation> biomeIds = parseBiomeIds(
            definitionId, json, errors, context
        );
        boolean enabled = ExplorerJsonValidation.optionalBoolean(
            definitionId, json, "enabled", true, errors
        );
        Integer dwellTicks = ExplorerJsonValidation.optionalInt(
            definitionId,
            json,
            "dwell_ticks",
            DEFAULT_DWELL_TICKS,
            errors
        );
        String translationKey = ExplorerJsonValidation.optionalString(
            definitionId, json, "translation_key", errors
        );
        if (dwellTicks != null && dwellTicks < 0) {
            errors.add(definitionId, "dwell_ticks must be at least 0");
        }

        ExperienceSpec experience = null;
        if (enabled) {
            ExplorerParseResult<ExperienceSpec> parsed = ExperienceSpec.parse(
                definitionId, json, false, errors
            );
            experience = parsed.value();
        } else {
            validateDisabledExperience(definitionId, json, errors);
        }

        if (biomeIds == null || biomeIds.isEmpty() || dwellTicks == null
            || errors.messageCount() != errorCount) {
            return null;
        }
        return new BiomeDiscoveryRule(
            definitionId,
            biomeIds,
            enabled,
            experience,
            dwellTicks,
            Optional.ofNullable(translationKey)
        );
    }

    private static void validateRuleFields(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors,
        String context,
        boolean allowFormatVersion
    ) {
        for (String field : json.keySet()) {
            if (!RULE_FIELDS.contains(field)
                && !(allowFormatVersion && field.equals("format_version"))) {
                errors.add(
                    definitionId,
                    context + " has unknown field: " + field
                );
            }
        }
    }

    private static Set<ResourceLocation> parseBiomeIds(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors,
        String context
    ) {
        boolean hasBiome = json.has("biome");
        boolean hasBiomes = json.has("biomes");
        if (hasBiome == hasBiomes) {
            errors.add(
                definitionId,
                context + " must contain exactly one of biome or biomes"
            );
            return null;
        }
        if (hasBiome) {
            ResourceLocation biomeId = ExplorerJsonValidation.requiredId(
                definitionId, json, "biome", errors
            );
            return biomeId == null ? null : Set.of(biomeId);
        }

        JsonElement element = json.get("biomes");
        if (!element.isJsonArray()) {
            errors.add(definitionId, context + ".biomes must be an array");
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            errors.add(
                definitionId, context + ".biomes must not be empty"
            );
            return null;
        }
        Set<ResourceLocation> result = new HashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement value = array.get(index);
            if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
                errors.add(
                    definitionId,
                    context + ".biomes[" + index
                        + "] must be a resource location string"
                );
                continue;
            }
            String text = value.getAsString();
            ResourceLocation id = ResourceLocation.tryParse(text);
            if (id == null) {
                errors.add(
                    definitionId,
                    context + ".biomes[" + index
                        + "] is not a valid resource location"
                );
            } else if (!result.add(id)) {
                errors.add(
                    definitionId,
                    context + " contains duplicate biome: " + id
                );
            }
        }
        return result;
    }

    static void validateDisabledExperience(
        ResourceLocation id,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        ResourceLocation tierId = null;
        if (json.has("experience_tier")) {
            tierId = ExplorerJsonValidation.requiredId(
                id, json, "experience_tier", errors
            );
        }
        if (json.has("xp")) {
            Integer xp = ExplorerJsonValidation.requiredInt(
                id, json, "xp", errors
            );
            if (xp != null && xp < 0) {
                errors.add(id, "xp must be at least 0");
            }
            if (!ExperienceSpec.SPECIAL.equals(tierId)) {
                errors.add(
                    id,
                    "xp is only allowed with craftbound:special"
                );
            }
        }
    }
}
