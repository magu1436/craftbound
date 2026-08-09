package com.magu1436.craftbound.occupations.foodproducer.quality;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageItemData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** オンライン中のプレイヤーインベントリだけ品質時計を進める. */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerInventoryQualityEvents {

    private static final double PLAYER_INVENTORY_MULTIPLIER = 1.0D;
    private static final int UPDATE_INTERVAL_TICKS = 20;

    private PlayerInventoryQualityEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        advanceInventory(player);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resetInventoryClock(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            pauseInventoryClock(player);
        }
    }

    /** 地面に存在した時間を加算せず、拾った時点から時計を再開する. */
    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            preparePickedUpStack(player, event.getItem().getItem());
        }
    }

    /** 投棄する直前までを精算し、地面にある間は時計を停止する. */
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            ItemStack stack = event.getEntity().getItem();
            long gameTime = player.serverLevel().getGameTime();
            advanceStack(stack, gameTime);
            pauseStack(stack, gameTime);
        }
    }

    /** 地面上のアイテムは品質劣化の対象外とし、次の管理先へ入るまで時計を停止する. */
    @SubscribeEvent
    public static void onItemEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity itemEntity) {
            pauseStack(itemEntity.getItem(), event.getLevel().getGameTime());
        }
    }

    private static void advanceInventory(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        forEachInventoryStack(player.getInventory(), stack -> advanceStack(stack, gameTime));
    }

    private static void resetInventoryClock(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        forEachInventoryStack(player.getInventory(), stack -> resetStack(stack, gameTime));
    }

    private static void pauseInventoryClock(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        forEachInventoryStack(player.getInventory(), stack -> pauseStack(stack, gameTime));
    }

    private static void forEachInventoryStack(Inventory inventory, java.util.function.Consumer<ItemStack> action) {
        inventory.items.forEach(action);
        inventory.armor.forEach(action);
        inventory.offhand.forEach(action);
    }

    /** 拾得前に統合先と時計を揃え、空きスロットがなくてもバニラ回収を成立させる. */
    private static void preparePickedUpStack(ServerPlayer player, ItemStack pickedUp) {
        long gameTime = player.serverLevel().getGameTime();
        Inventory inventory = player.getInventory();
        for (ItemStack existing : inventory.items) {
            if (prepareExistingStack(existing, pickedUp, inventory, gameTime)) {
                return;
            }
        }
        for (ItemStack existing : inventory.offhand) {
            if (prepareExistingStack(existing, pickedUp, inventory, gameTime)) {
                return;
            }
        }
        resetStack(pickedUp, gameTime);
    }

    private static boolean prepareExistingStack(
            ItemStack existing,
            ItemStack pickedUp,
            Inventory inventory,
            long gameTime
    ) {
        int limit = Math.min(inventory.getMaxStackSize(), existing.getMaxStackSize());
        return !existing.isEmpty()
                && existing.getCount() < limit
                && FoodQualityData.prepareForMerge(
                        existing,
                        pickedUp,
                        gameTime,
                        PLAYER_INVENTORY_MULTIPLIER
                );
    }

    private static void advanceStack(ItemStack stack, long gameTime) {
        FoodQualityData.advanceLoadedTime(stack, gameTime, PLAYER_INVENTORY_MULTIPLIER);
        PreservationStorageItemData.advanceLoadedTime(stack, gameTime);
    }

    private static void resetStack(ItemStack stack, long gameTime) {
        FoodQualityData.resetClock(stack, gameTime, PLAYER_INVENTORY_MULTIPLIER);
        PreservationStorageItemData.resetClock(stack, gameTime);
    }

    private static void pauseStack(ItemStack stack, long gameTime) {
        FoodQualityData.pauseClock(stack, gameTime, PLAYER_INVENTORY_MULTIPLIER);
        PreservationStorageItemData.pauseClock(stack, gameTime);
    }
}
