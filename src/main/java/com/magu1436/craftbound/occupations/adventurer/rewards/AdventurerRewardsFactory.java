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
        new AttributeRewardArgs("armor_reward", () -> Attributes.ARMOR, Operation.ADDITION),
        new AttributeRewardArgs("knockback_resistance_reward", () -> Attributes.KNOCKBACK_RESISTANCE, Operation.ADDITION),
        new AttributeRewardArgs("explosion_damage_reduction_reward", CraftboundAttributes.EXPLOSION_DAMAGE_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("projectile_damage_reduction_reward", CraftboundAttributes.PROJECTILE_DAMAGE_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("shield_footwork_reward", CraftboundAttributes.SHIELD_FOOTWORK::get, Operation.ADDITION),
        new AttributeRewardArgs("physical_resistance_reward", CraftboundAttributes.PHYSICAL_RESISTANCE::get, Operation.ADDITION),
        new AttributeRewardArgs("ranged_footwork_reward", CraftboundAttributes.RANGED_FOOTWORK::get, Operation.ADDITION),
        new AttributeRewardArgs("action_resistance_reward", CraftboundAttributes.ACTION_RESISTANCE::get, Operation.ADDITION),
        new AttributeRewardArgs("field_resupply_reward", CraftboundAttributes.FIELD_RESUPPLY::get, Operation.ADDITION),
        new AttributeRewardArgs("sensory_resistance_reward", CraftboundAttributes.SENSORY_RESISTANCE::get, Operation.ADDITION)
    );

    public static void registerRewards(){
        registerRewards(rewards);
        AttackSpeedReward.register();
        MaxHealthReward.register();
    }
}
