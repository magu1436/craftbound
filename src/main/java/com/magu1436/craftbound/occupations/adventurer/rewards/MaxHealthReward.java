package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.UUID;

import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

import com.magu1436.craftbound.common.AttributeReward;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.NonNullSupplier;

public class MaxHealthReward extends AttributeReward {

    private static final String REWARD_ID = "max_health_reward";
    private static final NonNullSupplier<? extends Attribute> ATTRIBUTE =
        () -> Attributes.MAX_HEALTH;

    protected MaxHealthReward(
        String rewardId,
        NonNullSupplier<? extends Attribute> attribute,
        UUID modifierId,
        double amount,
        Operation operation
    ) {
        super(
            rewardId,
            attribute,
            modifierId,
            amount,
            operation
        );
    }

    public static void register() {
        AttributeReward.register(
            REWARD_ID,
            ATTRIBUTE,
            Operation.ADDITION,
            (modifierId, amount) -> new MaxHealthReward(
                REWARD_ID,
                ATTRIBUTE,
                modifierId,
                amount,
                Operation.ADDITION
            )
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
}
