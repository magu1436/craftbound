package com.magu1436.craftbound.occupations.blacksmith.capability;

import com.magu1436.craftbound.common.SkillLevelState;
import com.magu1436.craftbound.common.capability.PlayerCapabilityData;

public interface IBlacksmithData extends PlayerCapabilityData<IBlacksmithData> {

    SkillLevelState getPrecisionScaleState();
    SkillLevelState getForceReadingState();
    SkillLevelState getStrikeReferenceState();
    SkillLevelState getPrecisionShapingState();
    SkillLevelState getToolPreservationState();
    SkillLevelState getMaterialInsightState();
    SkillLevelState getSmithingInstinctState();
    SkillLevelState getQualityAppraisalState();

    default int getPrecisionScaleLevel() {
        return getPrecisionScaleState().getLevel();
    }

    default int getForceReadingLevel() {
        return getForceReadingState().getLevel();
    }

    default boolean hasForceReadingLevel() {
        return getForceReadingState().isStageActive(1);
    }

    default int getStrikeReferenceLevel() {
        return getStrikeReferenceState().getLevel();
    }

    default boolean hasStrikeReferenceLevel() {
        return getStrikeReferenceState().isStageActive(1);
    }

    default int getPrecisionShapingLevel() {
        return getPrecisionShapingState().getLevel();
    }

    default int getToolPreservationLevel() {
        return getToolPreservationState().getLevel();
    }

    default int getMaterialInsightLevel() {
        return getMaterialInsightState().getLevel();
    }

    default int getSmithingInstinctLevel() {
        return getSmithingInstinctState().getLevel();
    }

    default int getQualityAppraisalLevel() {
        return getQualityAppraisalState().getLevel();
    }

}
