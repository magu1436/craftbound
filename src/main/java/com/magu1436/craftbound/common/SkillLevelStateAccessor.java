package com.magu1436.craftbound.common;

/**
 * Capabilityデータからスキル段階の状態を取得する。
 *
 * @param <C> Capabilityの公開データ型
 */
@FunctionalInterface
public interface SkillLevelStateAccessor<C> {
    SkillLevelState get(C data);
}
