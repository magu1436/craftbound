package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

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
        List<FoodCookingEffect> effects = FoodCookingData.effects(stack);
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (!level.isClientSide && quality != FoodQuality.SPOILED) {
            for (FoodCookingEffect stored : effects) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(stored.id());
                if (effect == null || stored.durationTicks() <= 0) {
                    continue;
                }
                entity.addEffect(new MobEffectInstance(
                        effect,
                        scaledDuration(stored.durationTicks(), quality),
                        stored.amplifier(),
                        false,
                        stored.showParticles(),
                        stored.showIcon()
                ));
            }
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
        FoodQuality quality = FoodQualityData.getOrStandard(stack);
        for (FoodCookingEffect stored : FoodCookingData.effects(stack)) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(stored.id());
            if (effect == null || stored.durationTicks() <= 0) {
                continue;
            }
            int durationSeconds = scaledDuration(stored.durationTicks(), quality) / 20;
            if (effect instanceof FoodRoleMobEffect roleEffect) {
                double amount = roleEffect.decodeEffectiveAmount(stored.amplifier());
                String displayAmount = formatAmount(
                        roleEffect.amountDisplay() == FoodRoleMobEffect.AmountDisplay.PERCENT
                                ? amount * 100.0D
                                : amount
                );
                tooltip.add(Component.translatable(
                        switch (roleEffect.amountDisplay()) {
                            case PERCENT -> "tooltip.craftbound.cooking.attribute_effect.percent";
                            case FLAT -> "tooltip.craftbound.cooking.attribute_effect.flat";
                            case BLOCKS -> "tooltip.craftbound.cooking.attribute_effect.blocks";
                        },
                        Component.translatable(effect.getDescriptionId()),
                        displayAmount,
                        durationSeconds
                ).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable(
                        "tooltip.craftbound.cooking.effect",
                        Component.translatable(effect.getDescriptionId()),
                        stored.amplifier() + 1,
                        durationSeconds
                ).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static int scaledDuration(int baseDuration, FoodQuality quality) {
        return Math.max(1, (int) Math.round(
                baseDuration * FoodQualityEffects.buffDurationMultiplier(quality)
        ));
    }

    private static String formatAmount(double amount) {
        return String.format(Locale.ROOT, "%.3f", amount)
                .replaceAll("\\.?0+$", "");
    }
}
