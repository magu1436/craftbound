package com.magu1436.craftbound.occupations.architect.rewards;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeRewardFactory;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraftforge.common.ForgeMod;

public class ArchitectRewardFactory extends AttributeRewardFactory {
    private static final List<AttributeRewardArgs> rewards = List.of(
        new AttributeRewardArgs("scaffolding_mobility", CraftboundAttributes.SCAFFOLDING_MOBILITY::get, Operation.ADDITION),
        new AttributeRewardArgs("demolition_speed_reward", CraftboundAttributes.DEMOLITION_SPEED::get, Operation.ADDITION),
        new AttributeRewardArgs("fall_damage_reduction_reward", CraftboundAttributes.FALL_DAMAGE_REDUCTION::get, Operation.ADDITION),
        new AttributeRewardArgs("firework_conservation_chance_reward", CraftboundAttributes.FIREWORK_CONSERVATION_CHANCE::get, Operation.ADDITION),
        new AttributeRewardArgs("placament_reach_reward", ForgeMod.BLOCK_REACH::get, Operation.MULTIPLY_TOTAL)
    );

    public static void registerRewards() {
        registerRewards(rewards);
    }
}
