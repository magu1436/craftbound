package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * 1種類のMobに対する討伐経験値の定義。
 */
public record MobExperienceDefinition(
    ResourceLocation entityId,
    EntityType<?> entityType,
    MobExperienceCategory category,
    int experience
) {
    public MobExperienceDefinition {
        Objects.requireNonNull(entityId, "entity id is null");
        Objects.requireNonNull(entityType, "entity type is null");
        Objects.requireNonNull(category, "category is null");

        if (experience < 0) {
            throw new IllegalArgumentException(
                "experience must be greater than or equal to 0"
            );
        }
    }
}
