package com.magu1436.craftbound.common;

import java.util.Objects;

import net.minecraftforge.common.capabilities.Capability;

/**
 * LevelRewardの登録に必要な情報。
 *
 * @param rewardId 報酬IDのパス
 * @param capability 対象となるCapability
 * @param stateAccessor Capabilityデータから対象スキルの状態を取得する関数
 * @param <C> Capabilityの公開データ型
 */
public record LevelRewardArgs<C>(
    String rewardId,
    Capability<C> capability,
    SkillLevelStateAccessor<C> stateAccessor
) {
    public LevelRewardArgs {
        Objects.requireNonNull(rewardId, "reward id is null");
        Objects.requireNonNull(capability, "capability is null");
        Objects.requireNonNull(stateAccessor, "state accessor is null");
    }
}
