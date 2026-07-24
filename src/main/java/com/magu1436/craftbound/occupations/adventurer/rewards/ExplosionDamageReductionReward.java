package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.UUID;

import com.magu1436.craftbound.common.AbilityRewardParsedValues;
import com.magu1436.craftbound.common.BasicAbilityReward;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

public class ExplosionDamageReductionReward extends BasicAbilityReward {

    private static final String REWARD_ID = "explosion_damage_reduction_reward";

    protected ExplosionDamageReductionReward(
        double amount,
        UUID modifierId
    ) {
        super(amount, modifierId);
    }

    public static void register() {
        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(REWARD_ID),
            context -> {
                AbilityRewardParsedValues jsonValues = ExplosionDamageReductionReward
                    .parseJson(context)
                    .getSuccessOrElse(null);
                if (jsonValues == null) return Result.failure(Problem.message("json value null"));
                return Result.success(new ExplosionDamageReductionReward(jsonValues.amount(), jsonValues.modifierId()));
            }
        );
    }

    @Override
    protected String getRewardId() {
        return REWARD_ID;
    }

    @Override
    protected Attribute getAttribute() {
        return CraftboundAttributes.EXPLOSION_DAMAGE_REDUCTION.get();
    }

}
