package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.UUID;

import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import com.magu1436.craftbound.common.AbilityRewardParsedValues;
import com.magu1436.craftbound.common.BasicAbilityReward;
import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class MaxHealthReward extends BasicAbilityReward {

    private static final String REWARD_ID = "max_health_reward";

    protected MaxHealthReward(double amount, UUID modifierId) {
        super(amount, modifierId);
    }

    public static void register() {
        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(REWARD_ID),
            context -> {
                AbilityRewardParsedValues jsonValues = MaxHealthReward
                    .parseJson(context)
                    .getSuccessOrElse(null);
                if (jsonValues == null) return Result.failure(Problem.message("json is null"));
                return Result.success(new MaxHealthReward(jsonValues.amount(), jsonValues.modifierId()));
            }
        );
    }

    /**
     * HP減少時に最大HPを超えないようにする調整するためのユーティリティメソッド
     */
    private void adjustHP(ServerPlayer player) {
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth > maxHealth) {
            player.setHealth(maxHealth);
        }
    }

    @Override
    public void update(RewardUpdateContext context) {
        super.update(context);
        this.adjustHP(context.getPlayer());
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        super.dispose(context);
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            this.adjustHP(player);
        }
    }

    @Override
    protected Attribute getAttribute() {
        return Attributes.MAX_HEALTH;
    }

    @Override
    protected String getRewardId() {
        return REWARD_ID;
    }
}
