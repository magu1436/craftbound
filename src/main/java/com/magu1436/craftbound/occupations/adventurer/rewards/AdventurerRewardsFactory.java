package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeRewardFactory;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.world.entity.ai.attributes.Attributes;

public class AdventurerRewardsFactory extends AttributeRewardFactory{
    private static final List<AttributeRewardArgs> rewards = List.of(
        new AttributeRewardArgs("attack_damage_reward", () -> Attributes.ATTACK_DAMAGE, Operation.ADDITION),
        new AttributeRewardArgs("attack_speed_reward", () -> Attributes.ATTACK_SPEED, Operation.ADDITION),
        new AttributeRewardArgs("armor_reward", () -> Attributes.ARMOR, Operation.ADDITION),
        new AttributeRewardArgs("knockback_resistance_reward", () -> Attributes.KNOCKBACK_RESISTANCE, Operation.ADDITION),
        new AttributeRewardArgs("explosion_damage_reduction_reward", CraftboundAttributes.EXPLOSION_DAMAGE_REDUCTION::get, Operation.ADDITION)
    );

    public static void registerRewards(){
        registerRewards(rewards);
        MaxHealthReward.register();
    }
}
