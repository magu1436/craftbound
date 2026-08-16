package com.magu1436.craftbound.integration.diet;

import java.util.Map;

import com.illusivesoulworks.diet.api.DietEvent;
import com.illusivesoulworks.diet.api.type.IDietTracker;
import com.illusivesoulworks.diet.common.capability.DietCapability;
import com.magu1436.craftbound.occupations.foodproducer.FoodProducerConfig;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingData;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDietValues;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDishItem;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Diet 2.1.xが存在するときだけロードするFoodProducer連携。 */
public final class FoodProducerDietIntegration {

    private FoodProducerDietIntegration() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(FoodProducerDietIntegration.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onConsume(DietEvent.ConsumeItemStack event) {
        if (!FoodProducerConfig.isDietIntegrationEnabled()) {
            return;
        }
        ItemStack stack = event.getStack();
        if (!(stack.getItem() instanceof FoodDishItem)) {
            return;
        }
        Map<String, Float> dietValues = FoodCookingData.dietValues(stack);
        if (dietValues.isEmpty()) {
            return;
        }
        DietCapability.get(event.getEntity()).ifPresent(tracker -> {
            if (apply(tracker, stack, dietValues)) {
                event.setCanceled(true);
            }
        });
    }

    private static boolean apply(
            IDietTracker tracker,
            ItemStack stack,
            Map<String, Float> dietValues
    ) {
        if (!tracker.isActive()) {
            return false;
        }
        Map<String, Float> current = tracker.getValues();
        double mealMultiplier = FoodCookingData.isPreserved(stack)
                ? FoodProducerConfig.preservedDietMultiplier()
                : FoodProducerConfig.professionalMealDietMultiplier();
        boolean changed = false;
        for (Map.Entry<String, Float> entry : dietValues.entrySet()) {
            String category = entry.getKey();
            if (!current.containsKey(category)) {
                continue;
            }
            float gain = FoodDietValues.scaledGain(
                    entry.getValue(),
                    FoodProducerConfig.dietGainPerPoint(),
                    mealMultiplier,
                    FoodProducerConfig.maximumDietGainPerGroup()
            );
            if (gain <= 0.0F) {
                continue;
            }
            tracker.setValue(category, Mth.clamp(current.get(category) + gain, 0.0F, 1.0F));
            changed = true;
        }
        if (changed) {
            tracker.addEaten(stack.getItem());
            tracker.sync();
        }
        return changed;
    }
}
