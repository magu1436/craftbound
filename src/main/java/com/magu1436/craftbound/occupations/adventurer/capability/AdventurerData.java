package com.magu1436.craftbound.occupations.adventurer.capability;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import com.magu1436.craftbound.common.SkillLevelState;

/**
 * 冒険家固有のプレイヤーデータの実装。
 */
public final class AdventurerData implements IAdventurerData {
    private static final String LAST_DEATHLINE_CROSSING_ACTIVATION_DAY_KEY =
        "LastDeathlineCrossingActivationDay";
    private static final long NEVER_ACTIVATED_DAY = Long.MIN_VALUE;

    private final SkillLevelState emergencyEvasionLevel =
        new SkillLevelState();
    private final SkillLevelState deathlineCrossingLevel =
        new SkillLevelState();

    private long emergencyEvasionCooldownEndTick;
    private long lastDeathlineCrossingActivationDay =
        NEVER_ACTIVATED_DAY;
    private long deathlineCrossingProtectionEndTick;

    @Override
    public SkillLevelState getEmergencyEvasionLevelState() {
        return emergencyEvasionLevel;
    }

    @Override
    public SkillLevelState getDeathlineCrossingLevelState() {
        return deathlineCrossingLevel;
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
    public long getLastDeathlineCrossingActivationDay() {
        return lastDeathlineCrossingActivationDay;
    }

    @Override
    public boolean canActivateDeathlineCrossing(long currentDay) {
        return currentDay != lastDeathlineCrossingActivationDay;
    }

    @Override
    public void recordDeathlineCrossingActivation(long currentDay) {
        lastDeathlineCrossingActivationDay = currentDay;
    }

    @Override
    public long getDeathlineCrossingProtectionEndTick() {
        return deathlineCrossingProtectionEndTick;
    }

    @Override
    public boolean isDeathlineCrossingProtected(long currentTick) {
        return currentTick < deathlineCrossingProtectionEndTick;
    }

    @Override
    public void startDeathlineCrossingProtection(
        long currentTick,
        int protectionTicks
    ) {
        if (protectionTicks < 0) {
            throw new IllegalArgumentException(
                "protection ticks must be greater than or equal to 0"
            );
        }

        deathlineCrossingProtectionEndTick = Math.max(
            deathlineCrossingProtectionEndTick,
            currentTick + protectionTicks
        );
    }

    @Override
    public CompoundTag savePersistentData() {
        CompoundTag tag = new CompoundTag();

        tag.putLong(
            LAST_DEATHLINE_CROSSING_ACTIVATION_DAY_KEY,
            lastDeathlineCrossingActivationDay
        );

        return tag;
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag is null");

        lastDeathlineCrossingActivationDay = tag.contains(
            LAST_DEATHLINE_CROSSING_ACTIVATION_DAY_KEY,
            Tag.TAG_LONG
        )
            ? tag.getLong(LAST_DEATHLINE_CROSSING_ACTIVATION_DAY_KEY)
            : NEVER_ACTIVATED_DAY;
    }

    @Override
    public void copyOnDeathFrom(IAdventurerData original) {
        Objects.requireNonNull(original, "original is null");

        emergencyEvasionLevel.copyFrom(
            original.getEmergencyEvasionLevelState()
        );
        deathlineCrossingLevel.copyFrom(
            original.getDeathlineCrossingLevelState()
        );
        emergencyEvasionCooldownEndTick =
            original.getEmergencyEvasionCooldownEndTick();
        lastDeathlineCrossingActivationDay =
            original.getLastDeathlineCrossingActivationDay();
        deathlineCrossingProtectionEndTick = 0L;
    }
}
