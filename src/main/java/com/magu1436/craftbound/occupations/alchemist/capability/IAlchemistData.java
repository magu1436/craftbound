package com.magu1436.craftbound.occupations.alchemist.capability;

import com.magu1436.craftbound.common.SkillLevelState;
import com.magu1436.craftbound.common.capability.PlayerCapabilityData;

public interface IAlchemistData extends PlayerCapabilityData<IAlchemistData> {

    SkillLevelState getBufferedStirringState();
    SkillLevelState getReactionAnalysisState();
    SkillLevelState getBatchHandlingState();
    SkillLevelState getProcessOptimizationState();
    SkillLevelState getRapidPreparationState();

    default int getBufferedStirringLevel() {
        return getBufferedStirringState().getLevel();
    }

    default int getReactionAnalysisLevel() {
        return getReactionAnalysisState().getLevel();
    }

    default int getBatchHandlingLevel() {
        return getBatchHandlingState().getLevel();
    }

    default int getProcessOptimizationLevel() {
        return getProcessOptimizationState().getLevel();
    }

    default int getRapidPreparationLevel() {
        return getRapidPreparationState().getLevel();
    }
}
