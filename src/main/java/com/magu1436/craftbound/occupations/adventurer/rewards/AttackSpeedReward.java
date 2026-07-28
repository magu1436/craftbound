package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import com.magu1436.craftbound.common.AttributeReward;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.puffish.skillsmod.api.reward.RewardDisposeContext;
import net.puffish.skillsmod.api.reward.RewardUpdateContext;

public class AttackSpeedReward extends AttributeReward {

    private static final String REWARD_ID = "attack_speed_reward";
    private static final Supplier<? extends Attribute> ATTRIBUTE =
        CraftboundAttributes.ATTACK_SPEED_BONUS::get;
    private static final UUID TOTAL_MODIFIER_ID =
        UUID.fromString("7d2b6a54-346f-43f0-b7c4-1bf3e397857c");

    protected AttackSpeedReward(
        UUID modifierId,
        double amount
    ) {
        super(
            REWARD_ID,
            ATTRIBUTE,
            modifierId,
            amount,
            Operation.ADDITION
        );
    }

    public static void register() {
        AttributeReward.register(
            REWARD_ID,
            ATTRIBUTE,
            Operation.ADDITION,
            AttackSpeedReward::new
        );
    }

    @Override
    public void update(RewardUpdateContext context) {
        super.update(context);
        rebuildAttackSpeed(context.getPlayer());
    }

    @Override
    public void dispose(RewardDisposeContext context) {
        super.dispose(context);
        for (ServerPlayer player : context.getServer().getPlayerList().getPlayers()) {
            rebuildAttackSpeed(player);
        }
    }

    private static void rebuildAttackSpeed(ServerPlayer player) {
        double bonusRate = player.getAttributeValue(
            Objects.requireNonNull(
                ATTRIBUTE.get(),
                "Attack speed bonus attribute is null"
            )
        );

        AttributeInstance attackSpeed = Objects.requireNonNull(
            player.getAttribute(Attributes.ATTACK_SPEED),
            "Attack speed attribute is null"
        );

        attackSpeed.removeModifier(TOTAL_MODIFIER_ID);

        if (bonusRate <= 0.0D) {
            return;
        }

        attackSpeed.addTransientModifier(
            new AttributeModifier(
                TOTAL_MODIFIER_ID,
                REWARD_ID,
                bonusRate,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            )
        );
    }
}
