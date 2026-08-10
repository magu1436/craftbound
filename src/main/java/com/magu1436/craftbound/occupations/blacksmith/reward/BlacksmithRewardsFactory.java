package com.magu1436.craftbound.occupations.blacksmith.reward;

import java.util.List;

import com.magu1436.craftbound.common.LevelRewardArgs;
import com.magu1436.craftbound.common.LevelRewardFactory;
import com.magu1436.craftbound.occupations.blacksmith.capability.IBlacksmithData;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

public class BlacksmithRewardsFactory {
    private static final List<LevelRewardArgs<IBlacksmithData>> rewards = List.of(
        new LevelRewardArgs<>(
            "precision_scale",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getPrecisionScaleState
        ),
        new LevelRewardArgs<>(
            "force_reading",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getForceReadingState
        ),
        new LevelRewardArgs<>(
            "strike_reference",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getStrikeReferenceState
        ),
        new LevelRewardArgs<>(
            "precision_shaping",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getPrecisionShapingState
        ),
        new LevelRewardArgs<>(
            "tool_preservation",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getToolPreservationState
        ),
        new LevelRewardArgs<>(
            "material_insight",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getMaterialInsightState
        ),
        new LevelRewardArgs<>(
            "smithing_instinct",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getSmithingInstinctState
        ),
        new LevelRewardArgs<>(
            "quality_appraisal",
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA,
            IBlacksmithData::getQualityAppraisalState
        )
    );

    public static void registerRewards() {
        LevelRewardFactory.registerRewards(rewards);
    }
}