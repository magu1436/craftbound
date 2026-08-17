package com.magu1436.craftbound.occupations.blacksmith.quality;

import java.util.Arrays;
import java.util.Optional;

public enum QualityPerformanceType {
    ATTACK_DAMAGE("attack_damage"),
    PROJECTILE_DAMAGE("projectile_damage"),
    ARMOR("armor"),
    ARMOR_TOUGHNESS("armor_toughness"),
    MINING_SPEED("mining_speed"),
    MAX_DURABILITY("max_durability");

    private final String serializedName;

    QualityPerformanceType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static Optional<QualityPerformanceType> fromSerializedName(String name) {
        return Arrays.stream(values())
            .filter(type -> type.serializedName.equals(name))
            .findFirst();
    }
}
