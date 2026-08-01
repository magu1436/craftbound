package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Set;

import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

/** ファイルIDで識別される通常の経験値ランク。 */
public record ExperienceTier(ResourceLocation id, int xp) {
    public static ExplorerParseResult<ExperienceTier> parse(
        ResourceLocation definitionId,
        JsonObject json,
        ExplorerDefinitionErrors errors
    ) {
        boolean valid = ExplorerJsonValidation.validateHeader(
            definitionId,
            json,
            Set.of("format_version", "xp"),
            errors
        );
        Integer xp = ExplorerJsonValidation.requiredInt(
            definitionId,
            json,
            "xp",
            errors
        );
        if (xp == null || xp < 0) {
            if (xp != null) {
                errors.add(definitionId, "xp must be at least 0");
            }
            valid = false;
        }
        return valid
            ? ExplorerParseResult.success(new ExperienceTier(definitionId, xp))
            : ExplorerParseResult.failure();
    }
}
