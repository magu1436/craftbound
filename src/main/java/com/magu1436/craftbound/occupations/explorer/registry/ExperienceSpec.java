package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Map;
import java.util.OptionalInt;

import com.google.gson.JsonObject;
import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.resources.ResourceLocation;

/** 通常ランクまたはspecial固定値による経験値指定。 */
public record ExperienceSpec(
    ResourceLocation tierId,
    OptionalInt specialXp
) {
    public static final ResourceLocation SPECIAL =
        CraftboundUtilities.createResourceLocation("special");
    public static final ResourceLocation DEFAULT =
        CraftboundUtilities.createResourceLocation("common");

    public static ExplorerParseResult<ExperienceSpec> parse(
        ResourceLocation definitionId,
        JsonObject json,
        boolean tierRequired,
        ExplorerDefinitionErrors errors
    ) {
        ResourceLocation tierId = null;
        if (json.has("experience_tier")) {
            tierId = ExplorerJsonValidation.requiredId(
                definitionId,
                json,
                "experience_tier",
                errors
            );
            if (tierId == null) {
                return ExplorerParseResult.failure();
            }
        } else if (tierRequired) {
            errors.add(definitionId, "experience_tier is required");
            return ExplorerParseResult.failure();
        } else {
            tierId = DEFAULT;
        }

        if (SPECIAL.equals(tierId)) {
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
                return ExplorerParseResult.failure();
            }
            return ExplorerParseResult.success(
                new ExperienceSpec(tierId, OptionalInt.of(xp))
            );
        }

        if (json.has("xp")) {
            errors.add(
                definitionId,
                "xp is only allowed with craftbound:special"
            );
            return ExplorerParseResult.failure();
        }
        return ExplorerParseResult.success(
            new ExperienceSpec(tierId, OptionalInt.empty())
        );
    }

    public OptionalInt resolveXp(
        Map<ResourceLocation, ExperienceTier> tiers
    ) {
        if (SPECIAL.equals(tierId)) {
            return specialXp;
        }
        ExperienceTier tier = tiers.get(tierId);
        return tier == null ? OptionalInt.empty() : OptionalInt.of(tier.xp());
    }
}
