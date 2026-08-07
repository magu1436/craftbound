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

/** 同じ設定を共有する1件以上の構造物IDまたはタグの定義。 */
public record StructureDiscoveryRule(
    ResourceLocation definitionId,
    Set<StructureSelector> selectors,
    boolean enabled,
    ExperienceSpec experience,
    int dwellTicks,
    int maxDiscoveries,
    Optional<String> translationKey
) {
    public static final int DEFAULT_DWELL_TICKS = 100;
    public static final int DEFAULT_MAX_DISCOVERIES = 5;
    private static final Set<String> RULE_FIELDS = Set.of(
        "structure", "structures", "enabled", "experience_tier",
        "xp", "dwell_ticks", "max_discoveries", "translation_key"
    );
    private static final Set<String> LEGACY_FILE_FIELDS = Set.of(
        "format_version", "structure", "structures", "enabled",
        "experience_tier", "xp", "dwell_ticks", "max_discoveries",
        "translation_key"
    );
    private static final Set<String> GROUPED_FILE_FIELDS = Set.of(
        "format_version", "rules"
    );

    public StructureDiscoveryRule {
        selectors = Set.copyOf(selectors);
        if (selectors.isEmpty()) {
            throw new IllegalArgumentException(
                "structure selectors must not be empty"
            );
        }
    }

    public static List<StructureDiscoveryRule> parseFile(
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

        List<StructureDiscoveryRule> result = new ArrayList<>();
        for (int index = 0; index < rules.size(); index++) {
            JsonElement element = rules.get(index);
            if (!element.isJsonObject()) {
                errors.add(
                    definitionId,
                    "rules[" + index + "] must be an object"
                );
                continue;
            }
            StructureDiscoveryRule rule = parseRule(
                definitionId, element.getAsJsonObject(), errors,
                "rules[" + index + "]", false
            );
            if (rule != null) {
                result.add(rule);
            }
        }
        return List.copyOf(result);
    }

    private static List<StructureDiscoveryRule> parseLegacyFile(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, LEGACY_FILE_FIELDS, errors
        );
        StructureDiscoveryRule rule = parseRule(
            definitionId, json, errors, "structure rule", true
        );
        return rule == null || errors.messageCount() != errorCount
            ? List.of()
            : List.of(rule);
    }

    private static StructureDiscoveryRule parseRule(
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
        Set<StructureSelector> selectors = parseSelectors(
            definitionId, json, errors, context
        );
        boolean enabled = ExplorerJsonValidation.optionalBoolean(
            definitionId, json, "enabled", true, errors
        );
        Integer dwellTicks = ExplorerJsonValidation.optionalInt(
            definitionId, json, "dwell_ticks", DEFAULT_DWELL_TICKS, errors
        );
        Integer maxDiscoveries = ExplorerJsonValidation.optionalInt(
            definitionId,
            json,
            "max_discoveries",
            DEFAULT_MAX_DISCOVERIES,
            errors
        );
        String translationKey = ExplorerJsonValidation.optionalString(
            definitionId, json, "translation_key", errors
        );
        if (dwellTicks != null && dwellTicks < 0) {
            errors.add(definitionId, "dwell_ticks must be at least 0");
        }
        if (maxDiscoveries != null && maxDiscoveries < -1) {
            errors.add(
                definitionId,
                "max_discoveries must be -1 or greater"
            );
        }

        ExperienceSpec experience = null;
        if (enabled) {
            experience = ExperienceSpec.parse(
                definitionId, json, true, errors
            ).value();
        } else {
            BiomeDiscoveryRule.validateDisabledExperience(
                definitionId, json, errors
            );
        }
        if (selectors == null || selectors.isEmpty() || dwellTicks == null
            || maxDiscoveries == null
            || errors.messageCount() != errorCount) {
            return null;
        }
        return new StructureDiscoveryRule(
            definitionId,
            selectors,
            enabled,
            experience,
            dwellTicks,
            maxDiscoveries,
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

    private static Set<StructureSelector> parseSelectors(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors,
        String context
    ) {
        boolean hasStructure = json.has("structure");
        boolean hasStructures = json.has("structures");
        if (hasStructure == hasStructures) {
            errors.add(
                definitionId,
                context
                    + " must contain exactly one of structure or structures"
            );
            return null;
        }
        if (hasStructure) {
            String text = ExplorerJsonValidation.requiredString(
                definitionId, json, "structure", errors
            );
            StructureSelector selector = text == null
                ? null : StructureSelector.parse(text);
            if (text != null && selector == null) {
                errors.add(definitionId, "structure selector is invalid");
            }
            return selector == null ? null : Set.of(selector);
        }

        JsonElement element = json.get("structures");
        if (!element.isJsonArray()) {
            errors.add(
                definitionId, context + ".structures must be an array"
            );
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.isEmpty()) {
            errors.add(
                definitionId, context + ".structures must not be empty"
            );
            return null;
        }
        Set<StructureSelector> result = new HashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonElement value = array.get(index);
            if (!value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString()) {
                errors.add(
                    definitionId,
                    context + ".structures[" + index
                        + "] must be a structure selector string"
                );
                continue;
            }
            String text = value.getAsString();
            StructureSelector selector = StructureSelector.parse(text);
            if (selector == null) {
                errors.add(
                    definitionId,
                    context + ".structures[" + index
                        + "] is not a valid structure selector"
                );
            } else if (!result.add(selector)) {
                errors.add(
                    definitionId,
                    context + " contains duplicate structure selector: "
                        + text
                );
            }
        }
        return result;
    }

    public sealed interface StructureSelector
        permits StructureSelector.Direct, StructureSelector.Tag {

        static StructureSelector parse(String value) {
            boolean tag = value.startsWith("#");
            String idText = tag ? value.substring(1) : value;
            ResourceLocation id = ResourceLocation.tryParse(idText);
            if (id == null || idText.isBlank()) {
                return null;
            }
            return tag ? new Tag(id) : new Direct(id);
        }

        record Direct(ResourceLocation structureId)
            implements StructureSelector {
        }

        record Tag(ResourceLocation tagId) implements StructureSelector {
        }
    }
}
