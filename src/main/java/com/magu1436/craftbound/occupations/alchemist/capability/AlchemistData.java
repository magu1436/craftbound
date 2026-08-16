package com.magu1436.craftbound.occupations.alchemist.capability;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.magu1436.craftbound.common.SkillLevelState;

import net.minecraft.nbt.CompoundTag;

public class AlchemistData implements IAlchemistData {

    public final SkillLevelState bufferedStirringState = new SkillLevelState();
    public final SkillLevelState reactionAnalysisState = new SkillLevelState();
    public final SkillLevelState batchHandlingState = new SkillLevelState();
    public final SkillLevelState rapidPreparationState = new SkillLevelState();

    @Override
    public SkillLevelState getBufferedStirringState() {
        return bufferedStirringState;
    }

    @Override
    public SkillLevelState getReactionAnalysisState() {
        return reactionAnalysisState;
    }

    @Override
    public SkillLevelState getBatchHandlingState(){
        return batchHandlingState;
    }

    @Override
    public SkillLevelState getRapidPreparationState(){
        return rapidPreparationState;
    }

    @Override
    public CompoundTag savePersistentData() {
        return new CompoundTag();
    }

    @Override
    public void loadPersistentData(@Nonnull CompoundTag tag){}

    @Override
    public void copyOnDeathFrom(@Nonnull IAlchemistData original) {
        
        bufferedStirringState.copyFrom(
            Objects.requireNonNull(
                original.getBufferedStirringState(),
                "bufferedStirringState is null"
            )
        );
        reactionAnalysisState.copyFrom(
            Objects.requireNonNull(
                original.getReactionAnalysisState(),
                "reactionAnalysis is null"
            )
        );
        batchHandlingState.copyFrom(
            Objects.requireNonNull(
                original.getBatchHandlingState(),
                "batchHandlingState is null"
            )
        );
        rapidPreparationState.copyFrom(
            Objects.requireNonNull(
                original.getRapidPreparationState(),
                "rapidPreparationState is null"
            )
        );
    }
}
