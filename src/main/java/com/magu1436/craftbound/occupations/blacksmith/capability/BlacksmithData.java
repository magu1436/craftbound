package com.magu1436.craftbound.occupations.blacksmith.capability;

import java.util.Objects;

import net.minecraft.nbt.CompoundTag;

import com.magu1436.craftbound.common.SkillLevelState;

/**
 * 鍛冶師固有のプレイヤーデータの実装。
 */
public final class BlacksmithData implements IBlacksmithData {
    private final SkillLevelState precisionScaleLevel =
        new SkillLevelState();
    private final SkillLevelState forceReadingLevel =
        new SkillLevelState();
    private final SkillLevelState strikeReferenceLevel =
        new SkillLevelState();
    private final SkillLevelState precisionShapingLevel =
        new SkillLevelState();
    private final SkillLevelState toolPreservationLevel =
        new SkillLevelState();
    private final SkillLevelState materialInsightLevel =
        new SkillLevelState();
    private final SkillLevelState smithingInstinctLevel =
        new SkillLevelState();
    private final SkillLevelState qualityAppraisalLevel =
        new SkillLevelState();

    @Override
    public SkillLevelState getPrecisionScaleState() {
        return precisionScaleLevel;
    }

    @Override
    public SkillLevelState getForceReadingState() {
        return forceReadingLevel;
    }

    @Override
    public SkillLevelState getStrikeReferenceState() {
        return strikeReferenceLevel;
    }

    @Override
    public SkillLevelState getPrecisionShapingState() {
        return precisionShapingLevel;
    }

    @Override
    public SkillLevelState getToolPreservationState() {
        return toolPreservationLevel;
    }

    @Override
    public SkillLevelState getMaterialInsightState() {
        return materialInsightLevel;
    }

    @Override
    public SkillLevelState getSmithingInstinctState() {
        return smithingInstinctLevel;
    }

    @Override
    public SkillLevelState getQualityAppraisalState() {
        return qualityAppraisalLevel;
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
    public void copyOnDeathFrom(IBlacksmithData original) {
        Objects.requireNonNull(original, "original is null");

        precisionScaleLevel.copyFrom(
            original.getPrecisionScaleState()
        );
        forceReadingLevel.copyFrom(
            original.getForceReadingState()
        );
        strikeReferenceLevel.copyFrom(
            original.getStrikeReferenceState()
        );
        precisionShapingLevel.copyFrom(
            original.getPrecisionShapingState()
        );
        toolPreservationLevel.copyFrom(
            original.getToolPreservationState()
        );
        materialInsightLevel.copyFrom(
            original.getMaterialInsightState()
        );
        smithingInstinctLevel.copyFrom(
            original.getSmithingInstinctState()
        );
        qualityAppraisalLevel.copyFrom(
            original.getQualityAppraisalState()
        );
    }
}