package com.magu1436.craftbound.occupations.foodproducer.farming;

import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

public final class AgriculturalFertilizerItem extends Item {

    public AgriculturalFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.FARMLAND)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos position = context.getClickedPos();
        int current = FarmlandFertility.get(level, position);
        if (current >= FarmlandFertility.MAXIMUM) {
            return InteractionResult.PASS;
        }

        int recovery = FarmlandFertility.BASE_FERTILIZER_RECOVERY;
        if (context.getPlayer() instanceof ServerPlayer player
                && FoodProducerSkills.fertilityManagementRank(player) >= 1) {
            recovery = FarmlandFertility.FERTILITY_MANAGEMENT_I_RECOVERY;
        }

        int recovered = FarmlandFertility.recover(level, position, recovery);
        if (recovered <= 0) {
            return InteractionResult.PASS;
        }

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            FoodProducerExperience.add(player, 2);
        }
        level.levelEvent(1505, position.above(), 0);
        return InteractionResult.CONSUME;
    }
}
