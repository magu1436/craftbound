package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** ディメンション個別定義。 */
public record DimensionDiscoveryRule(
    ResourceLocation definitionId,
    ResourceLocation dimensionId,
    boolean enabled,
    ExperienceSpec experience,
    int dwellTicks,
    Optional<String> translationKey
) {
    public static final int DEFAULT_DWELL_TICKS = 60;
    private static final Set<String> FIELDS = Set.of(
        "format_version", "dimension", "enabled", "experience_tier",
        "xp", "dwell_ticks", "translation_key"
    );

    public static ExplorerParseResult<DimensionDiscoveryRule> parse(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, FIELDS, errors
        );
        ResourceLocation dimensionId = ExplorerJsonValidation.requiredId(
            definitionId, json, "dimension", errors
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
            experience = ExperienceSpec.parse(
                definitionId, json, true, errors
            ).value();
        } else {
            BiomeDiscoveryRule.validateDisabledExperience(
                definitionId, json, errors
            );
        }
        if (dimensionId == null || dwellTicks == null
            || errors.messageCount() != errorCount) {
            return ExplorerParseResult.failure();
        }
        return ExplorerParseResult.success(new DimensionDiscoveryRule(
            definitionId,
            dimensionId,
            enabled,
            experience,
            dwellTicks,
            Optional.ofNullable(translationKey)
        ));
    }
}
