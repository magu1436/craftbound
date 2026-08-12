package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.List;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 完成予定の料理情報と品質上限を保持する加熱前の素材セット。 */
public final class PreparedIngredientSetItem extends Item {

    public PreparedIngredientSetItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.craftbound.prepared_ingredient_set.named",
                Component.translatable(FoodCookingData.nameKey(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        FoodCookingData.recipeId(stack).ifPresent(id -> tooltip.add(
                Component.translatable("tooltip.craftbound.cooking.recipe", id.toString())
                        .withStyle(ChatFormatting.GRAY)
        ));
        FoodQuality cap = FoodCookingData.qualityCap(stack);
        tooltip.add(Component.translatable("tooltip.craftbound.cooking.quality_cap",
                Component.translatable(switch (cap) {
                    case HIGH -> "quality.craftbound.high";
                    case STANDARD -> "quality.craftbound.standard";
                    case LOW -> "quality.craftbound.low";
                    case SPOILED -> "quality.craftbound.spoiled";
                })).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.craftbound.processing.rating.success")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
