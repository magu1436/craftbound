package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityEffects;
import com.magu1436.craftbound.registry.CraftboundMobEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** NBTの料理定義から食事性能と固有バフを提供する共通完成料理。 */
public final class FoodDishItem extends Item {

    private static final String ACTIVE_PROFESSIONAL_MEAL = "craftbound_active_professional_meal";

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
        FoodProperties.Builder builder = new FoodProperties.Builder()
                .nutrition(nutrition)
                .saturationMod(saturationModifier);
        if (isProfessionalMeal(stack)) {
            builder.alwaysEat();
        }
        return builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean professionalMeal = isProfessionalMeal(stack);
        boolean markerActive = player.hasEffect(CraftboundMobEffects.PROFESSIONAL_MEAL_ACTIVE.get());
        if (level.isClientSide && professionalMeal && markerActive) {
            // The server owns the active recipe id. Delay the local eating animation until
            // the server accepts a refresh of the same meal; rejected meals stay motionless.
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide && professionalMeal && hasDifferentActiveMeal(player, stack)) {
            MobEffectInstance marker = player.getEffect(CraftboundMobEffects.PROFESSIONAL_MEAL_ACTIVE.get());
            int remainingSeconds = marker == null ? 0 : Math.max(1, marker.getDuration() / 20);
            player.displayClientMessage(Component.translatable(
                    "message.craftbound.cooking.professional_meal_active",
                    remainingSeconds
            ), true);
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        FoodQuality quality = FoodQualityData.getOrStandard(stack);
        List<FoodCookingEffect> effects = FoodCookingData.effects(stack);
        ResourceLocation recipeId = FoodCookingData.recipeId(stack).orElse(null);
        boolean professionalMeal = isProfessionalMeal(stack);
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
            if (entity instanceof Player player && recipeId != null && professionalMeal) {
                int duration = effects.stream()
                        .mapToInt(effect -> scaledDuration(effect.durationTicks(), quality))
                        .max()
                        .orElse(0);
                if (duration > 0) {
                    player.getPersistentData().putString(ACTIVE_PROFESSIONAL_MEAL, recipeId.toString());
                    player.addEffect(new MobEffectInstance(
                            CraftboundMobEffects.PROFESSIONAL_MEAL_ACTIVE.get(),
                            duration,
                            0,
                            false,
                            false,
                            false
                    ));
                }
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

    private static boolean isProfessionalMeal(ItemStack stack) {
        return ProfessionalMealPolicy.isProfessionalMeal(
                FoodCookingData.isPreserved(stack),
                !FoodCookingData.effects(stack).isEmpty()
        );
    }

    private static boolean hasDifferentActiveMeal(Player player, ItemStack candidate) {
        boolean markerActive = player.hasEffect(CraftboundMobEffects.PROFESSIONAL_MEAL_ACTIVE.get());
        if (!markerActive) {
            player.getPersistentData().remove(ACTIVE_PROFESSIONAL_MEAL);
            return false;
        }
        String candidateId = FoodCookingData.recipeId(candidate)
                .map(ResourceLocation::toString)
                .orElse("");
        return ProfessionalMealPolicy.blocksCandidate(
                true,
                player.getPersistentData().getString(ACTIVE_PROFESSIONAL_MEAL),
                candidateId
        );
    }

    private static String formatAmount(double amount) {
        return String.format(Locale.ROOT, "%.3f", amount)
                .replaceAll("\\.?0+$", "");
    }
}
