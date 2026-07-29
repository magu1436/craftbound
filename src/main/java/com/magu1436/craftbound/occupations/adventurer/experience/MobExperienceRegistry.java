package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.entity.EntityType;

/**
 * 読み込み済みのMob経験値定義を保持する。
 */
public final class MobExperienceRegistry {
    private static volatile Map<
        EntityType<?>,
        MobExperienceDefinition
    > definitions = Map.of();

    private MobExperienceRegistry() {
    }

    public static Optional<MobExperienceDefinition> find(
        EntityType<?> entityType
    ) {
        return Optional.ofNullable(definitions.get(entityType));
    }

    /**
     * 定義が存在しない場合は0を返す。
     */
    public static int getExperience(EntityType<?> entityType) {
        return find(entityType)
            .map(MobExperienceDefinition::experience)
            .orElse(0);
    }

    public static Map<
        EntityType<?>,
        MobExperienceDefinition
    > snapshot() {
        return definitions;
    }

    static void replace(
        Map<EntityType<?>, MobExperienceDefinition> newDefinitions
    ) {
        definitions = Map.copyOf(newDefinitions);
    }
}
