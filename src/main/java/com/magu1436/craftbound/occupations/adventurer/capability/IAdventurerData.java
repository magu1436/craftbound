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

    long getEmergencyEvasionCooldownEndTick();

    boolean isEmergencyEvasionOnCooldown(long currentTick);

    void startEmergencyEvasionCooldown(
        long currentTick,
        int cooldownTicks
    );
}
