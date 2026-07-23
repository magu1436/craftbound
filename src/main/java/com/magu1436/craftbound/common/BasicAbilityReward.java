package com.magu1436.craftbound.common;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.puffish.skillsmod.api.json.JsonElement;
import net.puffish.skillsmod.api.reward.Reward;
import net.puffish.skillsmod.api.reward.RewardConfigContext;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

/**
 * プレイヤーの基礎能力を変更する報酬の基底クラス
 */
public abstract class BasicAbilityReward implements Reward {
    private static final String AMOUNT_KEY = "amount";
    private final double amount;

    protected BasicAbilityReward(
        double amount
    ) {
        this.amount = amount;
    };

    /**
     * {@code RewardConfigContext} から報酬量 {@code amount} を取得するためのユーティリティメソッド
     * @param context 報酬の設定情報
     * @return 報酬量を含む {@code Result}
     */
    protected static Result<Double, Problem> parseJson( RewardConfigContext context ) {
        JsonElement result = context.getData()
            .getSuccess()
            .orElse(null);
        if (result == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 0"));
        }

        var json = result
            .getAsObject()
            .getSuccess()
            .orElse(null);
        
        if (json == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 1"));
        }

        Double amount = json
            .getDouble(AMOUNT_KEY)
            .getSuccessOrElse(null);

        if (amount == null) {
            return Result.failure(Problem.message("Some error happened on parsing json. CODE: 2"));
        }

        return Result.success(amount);
    }

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
