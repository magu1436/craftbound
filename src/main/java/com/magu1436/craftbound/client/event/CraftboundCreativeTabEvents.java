package com.magu1436.craftbound.client.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundItems;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CraftboundCreativeTabEvents {

    private CraftboundCreativeTabEvents() {
    }

    @SubscribeEvent
    public static void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(CraftboundItems.RANCH_BLOCK);
            event.accept(CraftboundItems.COOKING_TABLE);
            event.accept(CraftboundItems.HAND_MILL);
            event.accept(CraftboundItems.DRYING_RACK);
            event.accept(CraftboundItems.COOKING_POT);
            event.accept(CraftboundItems.PRESERVATION_STORAGE_1);
            event.accept(CraftboundItems.PRESERVATION_STORAGE_2);
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CraftboundItems.COMPOST);
            event.accept(CraftboundItems.AGRICULTURAL_FERTILIZER);
            event.accept(CraftboundItems.COOKING_KNIFE);
            event.accept(CraftboundItems.BLACKSMITH_COOKING_KNIFE);
            event.accept(CraftboundItems.WHEAT_FLOUR);
            event.accept(CraftboundItems.DOUGH);
            event.accept(CraftboundItems.SLICED_MEAT);
            event.accept(CraftboundItems.GROUND_MEAT);
            event.accept(CraftboundItems.CHOPPED_VEGETABLE);
            event.accept(CraftboundItems.FRUIT_PIECES);
            event.accept(CraftboundItems.DRIED_MEAT);
            event.accept(CraftboundItems.DRIED_VEGETABLE);
            event.accept(CraftboundItems.DRIED_FRUIT);
        }
    }
}
