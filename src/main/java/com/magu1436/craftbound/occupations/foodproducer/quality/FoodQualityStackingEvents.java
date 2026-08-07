package com.magu1436.craftbound.occupations.foodproducer.quality;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemStackedOnOtherEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** カーソル操作で品質時計だけが異なるスタックを統合できるようにする. */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodQualityStackingEvents {

    private static final double NORMAL_STORAGE_MULTIPLIER = 1.0D;

    private FoodQualityStackingEvents() {
    }

    @SubscribeEvent
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        ItemStack carried = event.getCarriedItem();
        ItemStack stackedOn = event.getStackedOnItem();
        if (carried.isEmpty()
                || stackedOn.isEmpty()
                || !event.getSlot().mayPlace(carried)) {
            return;
        }

        int destinationLimit = Math.min(
                stackedOn.getMaxStackSize(),
                event.getSlot().getMaxStackSize(carried)
        );
        if (stackedOn.getCount() >= destinationLimit) {
            return;
        }

        // 個数移動とメニュー同期はバニラへ任せ、比較直前に時計NBTだけを揃える.
        FoodQualityData.prepareForMerge(
                stackedOn,
                carried,
                event.getPlayer().level().getGameTime(),
                NORMAL_STORAGE_MULTIPLIER
        );
    }
}
