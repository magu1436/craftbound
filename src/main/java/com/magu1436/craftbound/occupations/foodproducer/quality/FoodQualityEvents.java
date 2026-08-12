package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.ArrayList;
import java.util.List;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodQualityEvents {

    private FoodQualityEvents() {
    }

    /** バニラクラフトでは材料の最低品質だけを継承し、補正や経験値を発生させない. */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        Container inventory = event.getInventory();
        List<ItemStack> inputs = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inputs.add(inventory.getItem(slot));
        }
        FoodQualityData.inheritMinimum(inputs, event.getCrafting(), level.getGameTime());
    }
}
