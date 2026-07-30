package com.magu1436.craftbound.occupations.explorer.rewards;

import java.util.List;

import com.magu1436.craftbound.common.AttributeRewardArgs;
import com.magu1436.craftbound.common.AttributeReward.Operation;
import com.magu1436.craftbound.occupations.adventurer.rewards.AdventurerRewardsFactory;
import com.magu1436.craftbound.registry.CraftboundAttributes;

public class ExplorerRewardsFactory extends AdventurerRewardsFactory {
    private static final List<AttributeRewardArgs> rewards = List.of(
        new AttributeRewardArgs("expedition_endurance_reward", CraftboundAttributes.EXPEDITION_ENDURANCE::get, Operation.ADDITION),
        new AttributeRewardArgs("diving_reward", CraftboundAttributes.DIVING::get, Operation.ADDITION),
        new AttributeRewardArgs("climbing_reward", CraftboundAttributes.CLIMBING::get, Operation.ADDITION),
        new AttributeRewardArgs("tool_care_reward", CraftboundAttributes.TOOL_CARE::get, Operation.ADDITION),
        new AttributeRewardArgs("soul_sand_traversal_reward", CraftboundAttributes.SOUL_SAND_TRAVERSAL::get, Operation.ADDITION),
        new AttributeRewardArgs("bushwhacking_reward", CraftboundAttributes.BUSHWHACKING::get, Operation.ADDITION),
        new AttributeRewardArgs("powder_snow_traversal_reward", CraftboundAttributes.POWDER_SNOW_TRAVERSAL::get, Operation.ADDITION),
        new AttributeRewardArgs("sure_footed_reward", CraftboundAttributes.SURE_FOOTED::get, Operation.ADDITION),
        new AttributeRewardArgs("cold_adaptation_reward", CraftboundAttributes.COLD_ADAPTATION::get, Operation.ADDITION)
    );

    public static void registerRewards() {
        registerRewards(rewards);
    }
}
