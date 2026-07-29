package com.magu1436.craftbound.common;

import java.util.HashSet;
import java.util.Set;

/**
 * スキルを構成する各段階の有効状態を保持する。
 *
 * <p>同じ段階に対する更新を複数回受けても、現在レベルが重複して
 * 増減しないように段階番号を集合として管理する。</p>
 */
public final class SkillLevelState {
    private final Set<Integer> activeStages = new HashSet<>();

    /**
     * 指定した段階の有効状態を設定する。
     *
     * @param stage 段階番号
     * @param active 有効にする場合は {@code true}
     * @throws IllegalArgumentException 段階番号が1未満の場合
     */
    public void setStageActive(int stage, boolean active) {
        validateStage(stage);

        if (active) {
            activeStages.add(stage);
        } else {
            activeStages.remove(stage);
        }
    }

    /**
     * 指定した段階が有効かを返す。
     *
     * @param stage 段階番号
     * @return 有効な場合は {@code true}
     * @throws IllegalArgumentException 段階番号が1未満の場合
     */
    public boolean isStageActive(int stage) {
        validateStage(stage);
        return activeStages.contains(stage);
    }

    /**
     * 有効な段階数を現在のスキルレベルとして返す。
     *
     * @return 現在のスキルレベル
     */
    public int getLevel() {
        return activeStages.size();
    }

    /**
     * すべての段階を無効にする。
     */
    public void clear() {
        activeStages.clear();
    }

    private static void validateStage(int stage) {
        if (stage < 1) {
            throw new IllegalArgumentException(
                "stage must be greater than or equal to 1"
            );
        }
    }
}
