package com.magu1436.craftbound.occupations.explorer.rewards;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.occupations.adventurer.rewards.AdventurerRewardsFactory;

public class ExplorerRewardsFactory extends AdventurerRewardsFactory {
    private static final List<AttributeRewardArgs> rewards = List.of();

    public static void registerRewards() {
        registerRewards(rewards);
    }
}
