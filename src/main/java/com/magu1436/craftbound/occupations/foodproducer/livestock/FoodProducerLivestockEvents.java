package com.magu1436.craftbound.occupations.foodproducer.livestock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityRolls;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodProducerLivestockEvents {

    private static final Set<Item> RAW_MEATS = Set.of(
            Items.BEEF,
            Items.PORKCHOP,
            Items.MUTTON,
            Items.CHICKEN,
            Items.RABBIT
    );

    private FoodProducerLivestockEvents() {
    }

    /** 卵は取得経路にかかわらず、固定規則を優先して高品質で初期化する. */
    @SubscribeEvent
    public static void onItemEntityCreated(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ItemEntity itemEntity)
                || !itemEntity.getItem().is(Items.EGG)
                || FoodQualityData.hasQuality(itemEntity.getItem())) {
            return;
        }
        FoodQualityData.initialize(itemEntity.getItem(), FoodQuality.HIGH, level.getGameTime());
    }

    /** 成体の対象動物の肉へ食肉処理ランクと初期品質を適用する. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !isTargetAdult(event.getEntity())) {
            return;
        }

        ServerPlayer player = realPlayer(event.getSource().getEntity());
        int rank = player == null ? 0 : FoodProducerSkills.rank(player, FoodProducerSkills.MEAT_PROCESSING);
        Collection<ItemEntity> drops = event.getDrops();
        List<ItemStack> meatStacks = new ArrayList<>();
        drops.removeIf(itemEntity -> {
            if (RAW_MEATS.contains(itemEntity.getItem().getItem())) {
                meatStacks.add(itemEntity.getItem().copy());
                return true;
            }
            return false;
        });

        Item primaryMeat = primaryMeat(event.getEntity());
        if (player != null
                && primaryMeat != Items.AIR
                && level.random.nextInt(100) < rank * 20) {
            meatStacks.add(new ItemStack(primaryMeat));
        }

        for (ItemStack meatStack : meatStacks) {
            createQualityDrops(level, event.getEntity(), meatStack, rank, drops);
        }
        if (player != null) {
            FoodProducerExperience.add(player, 2);
        }
    }

    /** 成体のウシ・ムーシュルームから得る牛乳は高品質固定とする. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Cow cow)
                || cow.isBaby()
                || !event.getItemStack().is(Items.BUCKET)) {
            return;
        }

        if (event.getLevel() instanceof ServerLevel level) {
            ItemStack milk = new ItemStack(Items.MILK_BUCKET);
            FoodQualityData.initialize(milk, FoodQuality.HIGH, level.getGameTime());
            event.getEntity().setItemInHand(
                    event.getHand(),
                    ItemUtils.createFilledResult(event.getItemStack(), event.getEntity(), milk)
            );
            cow.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
        event.setCanceled(true);
    }

    private static void createQualityDrops(
            ServerLevel level,
            Entity source,
            ItemStack original,
            int rank,
            Collection<ItemEntity> output
    ) {
        Map<FoodQuality, Integer> counts = new EnumMap<>(FoodQuality.class);
        for (int index = 0; index < original.getCount(); index++) {
            counts.merge(FoodQualityRolls.roll(level.random, rank), 1, Integer::sum);
        }
        counts.forEach((quality, count) -> {
            ItemStack stack = original.copy();
            stack.setCount(count);
            FoodQualityData.initialize(stack, quality, level.getGameTime());
            ItemEntity itemEntity = new ItemEntity(level, source.getX(), source.getY(), source.getZ(), stack);
            itemEntity.setDefaultPickUpDelay();
            output.add(itemEntity);
        });
    }

    private static boolean isTargetAdult(Entity entity) {
        return entity instanceof AgeableMob ageable
                && !ageable.isBaby()
                && (entity instanceof Cow
                    || entity instanceof Pig
                    || entity instanceof Sheep
                    || entity instanceof Chicken
                    || entity instanceof Rabbit);
    }

    private static Item primaryMeat(Entity entity) {
        if (entity instanceof Cow) return Items.BEEF;
        if (entity instanceof Pig) return Items.PORKCHOP;
        if (entity instanceof Sheep) return Items.MUTTON;
        if (entity instanceof Chicken) return Items.CHICKEN;
        if (entity instanceof Rabbit) return Items.RABBIT;
        return Items.AIR;
    }

    private static ServerPlayer realPlayer(Entity entity) {
        return entity instanceof ServerPlayer player && !(player instanceof FakePlayer) ? player : null;
    }
}
