package com.magu1436.craftbound.occupations.alchemist.reward;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeRewardFactory;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.registry.CraftboundAttributes;

public class AlchemistRewardsFactory {
    private static final List<AttributeRewardArgs> attributeRewardArgs = List.of(
        new AttributeRewardArgs("precision_heat_reward", CraftboundAttributes.ALCHEMICAL_FINE_HEAT_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("thermal_retention_reward", CraftboundAttributes.ALCHEMICAL_THERMAL_RETENTION::get, Operation.ADDITION),
        new AttributeRewardArgs("process_optimization_reward", CraftboundAttributes.ALCHEMICAL_WAIT_TIME_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("stabilization_reward", CraftboundAttributes.ALCHEMICAL_AFTEREFFECT_REDUCTION::get, Operation.ADDITION)
    );

    public static void registerRewards() {
        AttributeRewardFactory.registerRewards(attributeRewardArgs);
    }
}
