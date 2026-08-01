package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 腐敗品の利用禁止と品質に応じた食事性能をサーバー側で適用する. */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodQualityUsageEvents {

    private static final Map<UUID, FoodUseSnapshot> FOOD_USE_SNAPSHOTS = new HashMap<>();

    private FoodQualityUsageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        ItemStack stack = event.getItem();
        if (event.getEntity() instanceof ServerPlayer player) {
            FOOD_USE_SNAPSHOTS.remove(player.getUUID());
        }
        if (event.getEntity().level() instanceof ServerLevel level) {
            FoodQualityData.advanceLoadedTime(stack, level.getGameTime(), 1.0D);
        }
        if (FoodQualityData.isSpoiled(stack)) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || !stack.isEdible()) {
            return;
        }
        FoodQualityData.get(stack).ifPresent(quality -> FOOD_USE_SNAPSHOTS.put(
                player.getUUID(),
                FoodUseSnapshot.capture(player, stack, quality)
        ));
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            FOOD_USE_SNAPSHOTS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FoodUseSnapshot snapshot = FOOD_USE_SNAPSHOTS.remove(player.getUUID());
        if (snapshot == null || snapshot.item() != event.getItem().getItem()) {
            return;
        }

        FoodData foodData = player.getFoodData();
        int nutrition = FoodQualityEffects.adjustedNutrition(event.getItem(), player, snapshot.quality());
        float saturationGain = FoodQualityEffects.adjustedSaturationGain(event.getItem(), player, snapshot.quality());
        int adjustedFoodLevel = Math.min(20, snapshot.foodLevel() + nutrition);
        float adjustedSaturation = Math.min(
                adjustedFoodLevel,
                snapshot.saturationLevel() + saturationGain
        );
        foodData.setFoodLevel(adjustedFoodLevel);
        foodData.setSaturation(adjustedSaturation);

    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAnimalInteract(PlayerInteractEvent.EntityInteract event) {
        advanceBeforeInteraction(event.getItemStack(), event.getEntity().level());
        if (!(event.getTarget() instanceof Animal) || !FoodQualityData.isSpoiled(event.getItemStack())) {
            return;
        }
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCampfireInteract(PlayerInteractEvent.RightClickBlock event) {
        advanceBeforeInteraction(event.getItemStack(), event.getLevel());
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof CampfireBlock)
                || !FoodQualityData.isSpoiled(event.getItemStack())) {
            return;
        }
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    private static void advanceBeforeInteraction(ItemStack stack, net.minecraft.world.level.Level level) {
        if (level instanceof ServerLevel serverLevel) {
            FoodQualityData.advanceLoadedTime(stack, serverLevel.getGameTime(), 1.0D);
        }
    }

    private record FoodUseSnapshot(
            Item item,
            FoodQuality quality,
            int foodLevel,
            float saturationLevel
    ) {
        static FoodUseSnapshot capture(ServerPlayer player, ItemStack stack, FoodQuality quality) {
            return new FoodUseSnapshot(
                    stack.getItem(),
                    quality,
                    player.getFoodData().getFoodLevel(),
                    player.getFoodData().getSaturationLevel()
            );
        }
    }
}
