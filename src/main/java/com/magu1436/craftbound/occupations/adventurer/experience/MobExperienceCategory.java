package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.Arrays;
import java.util.Optional;

/**
 * Mob経験値定義で使用する強さの区分。
 */
public enum MobExperienceCategory {
    PASSIVE("passive", true),
    HOSTILE("hostile", true),
    DANGEROUS("dangerous", true),
    STRONG("strong", false),
    ELITE("elite", false),
    MINOR_BOSS("minor_boss", false),
    BOSS("boss", false),
    HIGH_DIFFICULTY_BOSS("high_difficulty_boss", false),
    FINAL_BOSS("final_boss", false);

    private final String serializedName;
    private final boolean normalMob;

    MobExperienceCategory(
        String serializedName,
        boolean normalMob
    ) {
        this.serializedName = serializedName;
        this.normalMob = normalMob;
    }

    public String getSerializedName() {
        return serializedName;
    }

    /**
     * 最序盤だけ経験値を付与する通常Mob区分かを返す。
     */
    public boolean isNormalMob() {
        return normalMob;
    }

    public static Optional<MobExperienceCategory> fromSerializedName(
        String serializedName
    ) {
        return Arrays.stream(values())
            .filter(category ->
                category.serializedName.equals(serializedName)
            )
            .findFirst();
    }
}
