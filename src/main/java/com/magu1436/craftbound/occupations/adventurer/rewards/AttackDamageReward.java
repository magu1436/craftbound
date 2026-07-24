package com.magu1436.craftbound.occupations.adventurer.rewards;

import java.util.UUID;

import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.api.util.Problem;
import net.puffish.skillsmod.api.util.Result;

import com.magu1436.craftbound.common.AbilityRewardParsedValues;
import com.magu1436.craftbound.common.BasicAbilityReward;
import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AttackDamageReward extends BasicAbilityReward {

    private static final String REWARD_ID = "attack_damage_reward";

    protected AttackDamageReward(double amount, UUID modifierId) {
        super(amount, modifierId);
    }

    public static void register() {
        SkillsAPI.registerReward(
            CraftboundUtilities.createResourceLocation(REWARD_ID),
            context -> {
                AbilityRewardParsedValues jsonValues = AttackDamageReward
                    .parseJson(context)
                    .getSuccessOrElse(null);
                if (jsonValues == null) return Result.failure(Problem.message("json is null"));
                return Result.success(new AttackDamageReward(jsonValues.amount(), jsonValues.modifierId()));
            }
        );
    }

    @Override
    protected Attribute getAttribute() {
        return Attributes.ATTACK_DAMAGE;
    }

    @Override
    protected String getRewardId() {
        return REWARD_ID;
    }
}
