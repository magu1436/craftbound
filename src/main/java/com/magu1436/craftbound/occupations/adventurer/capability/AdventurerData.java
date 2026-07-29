package com.magu1436.craftbound.occupations.adventurer.capability;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

import com.magu1436.craftbound.common.SkillLevelState;

/**
 * 冒険家固有のプレイヤーデータの実装。
 */
public final class AdventurerData implements IAdventurerData {
    private final SkillLevelState emergencyEvasionLevel =
        new SkillLevelState();

    private long emergencyEvasionCooldownEndTick;

    @Override
    public SkillLevelState getEmergencyEvasionLevelState() {
        return emergencyEvasionLevel;
    }

    @Override
    public long getEmergencyEvasionCooldownEndTick() {
        return emergencyEvasionCooldownEndTick;
    }

    @Override
    public boolean isEmergencyEvasionOnCooldown(long currentTick) {
        return currentTick < emergencyEvasionCooldownEndTick;
    }

    @Override
    public void startEmergencyEvasionCooldown(
        long currentTick,
        int cooldownTicks
    ) {
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException(
                "cooldown ticks must be greater than or equal to 0"
            );
        }

        emergencyEvasionCooldownEndTick = Math.max(
            emergencyEvasionCooldownEndTick,
            currentTick + cooldownTicks
        );
    }

    @Override
    public CompoundTag savePersistentData() {
        return new CompoundTag();
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag is null");
    }

    @Override
    public void copyOnDeathFrom(IAdventurerData original) {
        Objects.requireNonNull(original, "original is null");

        emergencyEvasionLevel.copyFrom(
            original.getEmergencyEvasionLevelState()
        );
        emergencyEvasionCooldownEndTick =
            original.getEmergencyEvasionCooldownEndTick();
    }
}
