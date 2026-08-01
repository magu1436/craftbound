package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Optional;
import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** バイオーム個別定義。明示的な無効化も保持する。 */
public record BiomeDiscoveryRule(
    ResourceLocation definitionId,
    ResourceLocation biomeId,
    boolean enabled,
    ExperienceSpec experience,
    int dwellTicks,
    Optional<String> translationKey
) {
    public static final int DEFAULT_DWELL_TICKS = 100;
    private static final Set<String> FIELDS = Set.of(
        "format_version", "biome", "enabled", "experience_tier",
        "xp", "dwell_ticks", "translation_key"
    );

    public static ExplorerParseResult<BiomeDiscoveryRule> parse(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        int errorCount = errors.messageCount();
        ExplorerJsonValidation.validateHeader(
            definitionId, json, FIELDS, errors
        );
        ResourceLocation biomeId = ExplorerJsonValidation.requiredId(
            definitionId, json, "biome", errors
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

        if (biomeId == null || dwellTicks == null
            || errors.messageCount() != errorCount) {
            return ExplorerParseResult.failure();
        }
        return ExplorerParseResult.success(new BiomeDiscoveryRule(
            definitionId,
            biomeId,
            enabled,
            experience,
            dwellTicks,
            Optional.ofNullable(translationKey)
        ));
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
