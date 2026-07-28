package com.magu1436.craftbound.common;

import java.util.List;

public class AttributeRewardFactory {

    public static void registerReward(AttributeRewardArgs reward) {
        AttributeReward.register(reward.rewardId(), reward.attribute(), reward.operation());
    }

    public static void registerRewards(
        List<AttributeRewardArgs> rewards
    ) {
        for (AttributeRewardArgs args: rewards) {
            registerReward(args);
        }
    }
}
