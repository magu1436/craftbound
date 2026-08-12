package com.magu1436.craftbound.common;

import java.util.List;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

public final class PuffishSkillsUtilities {
    private PuffishSkillsUtilities() {
    }

    /**
     * スキルを取得するための関数.  
     * 指定のスキルが見つからない場合は {@code Optional.empty()} を返却する.  
     * @param categoryId スキルのカテゴリのID
     * @param skillId スキルのID
     * @return 指定のスキル. 存在しない場合は {@code Optinal.empty()}
     */
    public static Optional<Skill> getSkill (
        ResourceLocation categoryId,
        String skillId
    ) {
        return SkillsAPI.getCategory(categoryId)
            .flatMap(category -> category.getSkill(skillId));
    }

    /**
     * 指定のプレイヤーはスキルをアンロックしているかどうか確認するための関数.  
     * @param player 解除済みか確認したいプレイヤー
     * @param skill 対象のスキル
     * @return スキルを解除済みなら {@code true}
     */
    public static boolean hasSkill(
        ServerPlayer player,
        Skill skill
    ) {
        return skill.getState(player) == Skill.State.UNLOCKED;
    }

    /**
     * 指定のプレイヤーはスキルをアンロックしているかどうか確認するための関数.  
     * 存在しないスキルが指定された場合, {@code false} を返す.  
     * @param player 解除済みか確認したいプレイヤー
     * @param categoryId 対象のスキルが属するカテゴリのID
     * @param skillId 対象のスキルのID
     * @return スキルを解除済みなら {@code true}
     */
    public static boolean hasSkill(
        ServerPlayer player,
        ResourceLocation categoryId,
        String skillId
    ) {
        return getSkill(categoryId, skillId)
            .map(skill -> hasSkill(player, skill))
            .orElse(false);
    }

    /**
     * 段階別に別ノードとして定義されたスキルの最高取得ランクを返す.
     * リストはランクIから昇順で指定する.
     */
    public static int getHighestUnlockedRank(
        ServerPlayer player,
        ResourceLocation categoryId,
        List<String> rankedSkillIds
    ) {
        for (int index = rankedSkillIds.size() - 1; index >= 0; index--) {
            if (hasSkill(player, categoryId, rankedSkillIds.get(index))) {
                return index + 1;
            }
        }

        return 0;
    }
}
