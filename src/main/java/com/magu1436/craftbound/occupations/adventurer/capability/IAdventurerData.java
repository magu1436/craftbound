package com.magu1436.craftbound.occupations.adventurer.capability;

import com.magu1436.craftbound.common.SkillLevelState;
import com.magu1436.craftbound.common.capability.PlayerCapabilityData;

/**
 * 冒険家固有のプレイヤーデータ。
 */
public interface IAdventurerData
    extends PlayerCapabilityData<IAdventurerData> {

    SkillLevelState getEmergencyEvasionLevelState();

    default int getEmergencyEvasionLevel() {
        return getEmergencyEvasionLevelState().getLevel();
    }

    SkillLevelState getDeathlineCrossingLevelState();

    default int getDeathlineCrossingLevel() {
        return getDeathlineCrossingLevelState().getLevel();
    }

    default boolean hasDeathlineCrossing() {
        return getDeathlineCrossingLevelState().isStageActive(1);
    }

    long getEmergencyEvasionCooldownEndTick();

    boolean isEmergencyEvasionOnCooldown(long currentTick);

    void startEmergencyEvasionCooldown(
        long currentTick,
        int cooldownTicks
    );

    long getLastDeathlineCrossingActivationDay();

    boolean canActivateDeathlineCrossing(long currentDay);

    void recordDeathlineCrossingActivation(long currentDay);

    long getDeathlineCrossingProtectionEndTick();

    boolean isDeathlineCrossingProtected(long currentTick);

    void startDeathlineCrossingProtection(
        long currentTick,
        int protectionTicks
    );
}
