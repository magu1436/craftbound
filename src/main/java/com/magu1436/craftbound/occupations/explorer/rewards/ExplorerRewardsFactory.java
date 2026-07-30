package com.magu1436.craftbound.occupations.explorer.rewards;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.occupations.adventurer.rewards.AdventurerRewardsFactory;
import com.magu1436.craftbound.registry.CraftboundAttributes;

public class ExplorerRewardsFactory extends AdventurerRewardsFactory {
    private static final List<AttributeRewardArgs> rewards = List.of(
        new AttributeRewardArgs("expedition_endurance", CraftboundAttributes.EXPEDITION_ENDURANCE::get, Operation.ADDITION)
    );

    public static void registerRewards() {
        registerRewards(rewards);
    }
}
