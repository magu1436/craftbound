package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 品質を持つFoodProducer中間素材。元材料の種類はスタックNBTへ保持する。 */
public final class FoodIntermediateItem extends Item {

    public FoodIntermediateItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        FoodIntermediateData.sourceName(stack).ifPresent(name -> tooltip.add(
                Component.translatable("tooltip.craftbound.processing.source", name)
                        .withStyle(ChatFormatting.GRAY)
        ));
        if (FoodIntermediateData.hasSuccess(stack)) {
            tooltip.add(Component.translatable("tooltip.craftbound.processing.rating.success")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
