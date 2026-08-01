package com.magu1436.craftbound.occupations.foodproducer.quality;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 品質対象スタックの品質と品質時計をクライアントのツールチップへ表示する. */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FoodQualityTooltipEvents {

    private FoodQualityTooltipEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        FoodQualityData.get(stack).ifPresent(quality -> {
            event.getToolTip().add(Component.translatable(
                    "tooltip.craftbound.food_quality",
                    Component.translatable(qualityTranslationKey(quality)).withStyle(qualityColor(quality))
            ).withStyle(ChatFormatting.GRAY));

            if (quality != FoodQuality.SPOILED) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.craftbound.quality_remaining",
                        formatDuration(FoodQualityData.getRemainingRealTicks(stack))
                ).withStyle(ChatFormatting.DARK_GRAY));
                event.getToolTip().add(Component.translatable(
                        "tooltip.craftbound.spoilage_remaining",
                        formatDuration(FoodQualityData.getRemainingUntilSpoiledRealTicks(stack))
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (!FoodQualityData.isClockRunning(stack)) {
                event.getToolTip().add(Component.translatable(
                        FoodQualityData.isFrozenForTesting(stack)
                                ? "tooltip.craftbound.quality_test_frozen"
                                : "tooltip.craftbound.quality_clock_paused"
                ).withStyle(ChatFormatting.BLUE));
            }
            FoodProperties food = stack.getFoodProperties(event.getEntity());
            if (food != null && quality != FoodQuality.SPOILED) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.craftbound.adjusted_nutrition",
                        FoodQualityEffects.adjustedNutrition(stack, event.getEntity(), quality)
                ).withStyle(ChatFormatting.DARK_GREEN));
                event.getToolTip().add(Component.translatable(
                        "tooltip.craftbound.adjusted_saturation",
                        String.format("%.1f", FoodQualityEffects.adjustedSaturationGain(
                                stack,
                                event.getEntity(),
                                quality
                        ))
                ).withStyle(ChatFormatting.DARK_GREEN));
            }
        });
    }

    private static String qualityTranslationKey(FoodQuality quality) {
        return switch (quality) {
            case HIGH -> "quality.craftbound.high";
            case STANDARD -> "quality.craftbound.standard";
            case LOW -> "quality.craftbound.low";
            case SPOILED -> "quality.craftbound.spoiled";
        };
    }

    private static ChatFormatting qualityColor(FoodQuality quality) {
        return switch (quality) {
            case HIGH -> ChatFormatting.GOLD;
            case STANDARD -> ChatFormatting.WHITE;
            case LOW -> ChatFormatting.GRAY;
            case SPOILED -> ChatFormatting.DARK_RED;
        };
    }

    private static String formatDuration(double ticks) {
        long totalSeconds = Math.max(0L, (long) Math.ceil(ticks / 20.0D));
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

}
