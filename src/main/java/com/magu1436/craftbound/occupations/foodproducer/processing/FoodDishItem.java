package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.List;

import javax.annotation.Nullable;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** NBTの料理定義から食事性能と固有バフを提供する共通完成料理。 */
public final class FoodDishItem extends Item {

    public FoodDishItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(FoodCookingData.nameKey(stack));
    }

    @Nullable
    @Override
    public FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
        int nutrition = FoodCookingData.nutrition(stack);
        float saturationModifier = FoodCookingData.saturationGain(stack) / (nutrition * 2.0F);
        return new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturationModifier)
                .build();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodQuality quality = FoodQualityData.getOrStandard(stack);
        var effectId = FoodCookingData.effectId(stack);
        int amplifier = FoodCookingData.effectAmplifier(stack);
        int baseDuration = FoodCookingData.effectDuration(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && quality != FoodQuality.SPOILED && baseDuration > 0) {
            effectId.map(BuiltInRegistries.MOB_EFFECT::get)
                    .filter(effect -> effect != null)
                    .ifPresent(effect -> entity.addEffect(new MobEffectInstance(
                            effect,
                            Math.max(1, (int) Math.round(baseDuration
                                    * FoodQualityEffects.buffDurationMultiplier(quality))),
                            amplifier
                    )));
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.craftbound.cooking.food_values",
                FoodCookingData.nutrition(stack),
                FoodCookingData.saturationGain(stack)
        ).withStyle(ChatFormatting.GRAY));
        FoodCookingData.effectId(stack).ifPresent(id -> {
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect != null && FoodCookingData.effectDuration(stack) > 0) {
                tooltip.add(Component.translatable(
                        "tooltip.craftbound.cooking.effect",
                        Component.translatable(effect.getDescriptionId()),
                        FoodCookingData.effectAmplifier(stack) + 1,
                        FoodCookingData.effectDuration(stack) / 20
                ).withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
