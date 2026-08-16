package com.magu1436.craftbound.occupations.alchemist.reward;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeRewardFactory;
import com.magu1436.craftbound.common.LevelRewardArgs;
import com.magu1436.craftbound.common.LevelRewardFactory;
import com.magu1436.craftbound.common.SkillLevelStateAccessor;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.occupations.alchemist.capability.IAlchemistData;
import com.magu1436.craftbound.registry.CraftboundAttributes;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

public class AlchemistRewardsFactory {
    private static final List<AttributeRewardArgs> attributeRewardArgs = List.of(
        new AttributeRewardArgs("precision_heat_reward", CraftboundAttributes.ALCHEMICAL_FINE_HEAT_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("thermal_retention_reward", CraftboundAttributes.ALCHEMICAL_THERMAL_RETENTION::get, Operation.ADDITION),
        new AttributeRewardArgs("process_optimization_reward", CraftboundAttributes.ALCHEMICAL_WAIT_TIME_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("stabilization_reward", CraftboundAttributes.ALCHEMICAL_AFTEREFFECT_REDUCTION::get, Operation.ADDITION)
    );
    private static final List<LevelRewardArgs<IAlchemistData>> levelRewardArgs = List.of(
        createLevelRewardArgs(
            "buffered_stirring_reward",
            IAlchemistData::getBufferedStirringState
        ),
        createLevelRewardArgs(
            "reaction_analysis_reward", 
            IAlchemistData::getReactionAnalysisState
        ),
        createLevelRewardArgs(
            "batch_handling_reward", 
            IAlchemistData::getBatchHandlingState
        ),
        createLevelRewardArgs(
            "process_optimization_reward",
            IAlchemistData::getProcessOptimizationState
        ),
        createLevelRewardArgs(
            "rapid_preparation_reward",
            IAlchemistData::getRapidPreparationState
        )
    );

    public static void registerRewards() {
        AttributeRewardFactory.registerRewards(attributeRewardArgs);
        LevelRewardFactory.registerRewards(levelRewardArgs);
    }

    private static LevelRewardArgs<IAlchemistData> createLevelRewardArgs(@Nonnull String id, @Nonnull SkillLevelStateAccessor<IAlchemistData> accessor) {
        return new LevelRewardArgs<>(
            id,
            Objects.requireNonNull(CraftboundCapabilities.ALCHEMIST_DATA),
            accessor
        );
    }
}
