package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

/**
 * プレイヤーの基礎能力を変更する報酬の基底クラス
 */
public abstract class BasicAbilityReward implements Reward {
    private final double amount;

    BasicAbilityReward(
        double amount
    ) {
        this.amount = amount;
    };

    /**
     * @return 変更する属性
     */
    @Nonnull
    protected abstract Attribute getAttribute();

    /**
     * 
     * @return modifier の UUID
     */
    @Nonnull
    protected abstract UUID getModifierId();

    /**
     * 
     * @return 報酬のID
     */
    @Nonnull
    protected abstract String getRewardId();

    @Override
    public void update(RewardUpdateContext context) {
        int rewardCount = context.getCount();   // 同一報酬が取得された回数

        AttributeInstance attr = Objects.requireNonNull(
            context.getPlayer().getAttribute(this.getAttribute())
        );

        attr.removeModifier(this.getModifierId());  // 重複を防ぐために削除

        if (rewardCount <= 0) return;

        AttributeModifier modifier = new AttributeModifier(
            this.getModifierId(),
            this.getRewardId(),
            this.amount * rewardCount,
            AttributeModifier.Operation.ADDITION
        );

        attr.addTransientModifier(modifier);
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            Objects
                .requireNonNull(
                    player.getAttribute(this.getAttribute())
                )
                .removeModifier(this.getModifierId());
        }
    }
}
