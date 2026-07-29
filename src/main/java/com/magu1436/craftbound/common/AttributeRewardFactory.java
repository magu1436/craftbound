package com.magu1436.craftbound.common;

import java.util.List;
import java.util.Objects;

public class AttributeRewardFactory {

    public static void registerReward(AttributeRewardArgs reward) {
        Objects.requireNonNull(reward, "reward is null");

        AttributeReward.register(reward.rewardId(), reward.attribute(), reward.operation());
    }

    public static void registerRewards(
        List<AttributeRewardArgs> rewards
    ) {
        Objects.requireNonNull(rewards, "rewards is null");

        for (AttributeRewardArgs args : rewards) {
            registerReward(args);
        }
    }
}
