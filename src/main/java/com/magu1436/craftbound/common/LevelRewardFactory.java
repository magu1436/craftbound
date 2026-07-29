package com.magu1436.craftbound.common;

import java.util.List;
import java.util.Objects;

/**
 * LevelRewardの登録処理を提供する。
 */
public final class LevelRewardFactory {
    private LevelRewardFactory() {
    }

    /**
     * LevelRewardを1件登録する。
     *
     * @param reward 登録情報
     * @param <C> Capabilityの公開データ型
     */
    public static <C> void registerReward(
        LevelRewardArgs<C> reward
    ) {
        Objects.requireNonNull(reward, "reward is null");

        LevelReward.register(
            reward.rewardId(),
            reward.capability(),
            reward.stateAccessor()
        );
    }

    /**
     * 同一のCapability型を対象とするLevelRewardをまとめて登録する。
     *
     * @param rewards 登録情報の一覧
     * @param <C> Capabilityの公開データ型
     */
    public static <C> void registerRewards(
        List<LevelRewardArgs<C>> rewards
    ) {
        Objects.requireNonNull(rewards, "rewards is null");

        for (LevelRewardArgs<C> reward : rewards) {
            registerReward(reward);
        }
    }
}
