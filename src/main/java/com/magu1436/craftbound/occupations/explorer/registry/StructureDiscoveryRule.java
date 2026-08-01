package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** 構造物の個別IDまたはタグに対する定義。 */
public record StructureDiscoveryRule(
    ResourceLocation definitionId,
    StructureSelector selector,
    boolean enabled,
    ExperienceSpec experience,
    int dwellTicks,
    int maxDiscoveries,
    Optional<String> translationKey
) {
    public static final int DEFAULT_DWELL_TICKS = 100;
    public static final int DEFAULT_MAX_DISCOVERIES = 5;
    private static final Set<String> FIELDS = Set.of(
        "format_version", "structure", "enabled", "experience_tier",
        "xp", "dwell_ticks", "max_discoveries", "translation_key"
    );

    public static ExplorerParseResult<StructureDiscoveryRule> parse(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, FIELDS, errors
        );
        String selectorText = ExplorerJsonValidation.requiredString(
            definitionId, json, "structure", errors
        );
        StructureSelector selector = selectorText == null
            ? null
            : StructureSelector.parse(selectorText);
        if (selectorText != null && selector == null) {
            errors.add(definitionId, "structure selector is invalid");
        }
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
        if (selector == null || dwellTicks == null || maxDiscoveries == null
            || errors.messageCount() != errorCount) {
            return ExplorerParseResult.failure();
        }
        return ExplorerParseResult.success(new StructureDiscoveryRule(
            definitionId,
            selector,
            enabled,
            experience,
            dwellTicks,
            maxDiscoveries,
            Optional.ofNullable(translationKey)
        ));
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
