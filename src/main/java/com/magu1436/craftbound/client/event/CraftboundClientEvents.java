package com.magu1436.craftbound.client.event;

import java.util.List;
import java.util.function.Supplier;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class CraftboundClientEvents {

    private static final List<ScreenRegistration> SCREENS = List.of(
        // screen(
        //     CraftboundMenus.FORGE_STATION::get,
        //     ForgeStationScreen::new
        // )
    );

    private CraftboundClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(
        FMLClientSetupEvent event
    ) {
        event.enqueueWork(() ->
            SCREENS.forEach(ScreenRegistration::register)
        );
    }

    private static <
        M extends AbstractContainerMenu,
        S extends AbstractContainerScreen<M>
    > ScreenRegistration screen(
        Supplier<MenuType<M>> menuType,
        MenuScreens.ScreenConstructor<M, S> constructor
    ) {
        return new TypedScreenRegistration<>(
            menuType,
            constructor
        );
    }

    private interface ScreenRegistration {

        void register();
    }

    private record TypedScreenRegistration<
        M extends AbstractContainerMenu,
        S extends AbstractContainerScreen<M>
    >(
        Supplier<MenuType<M>> menuType,
        MenuScreens.ScreenConstructor<M, S> constructor
    ) implements ScreenRegistration {

        @Override
        public void register() {
            MenuScreens.register(
                this.menuType.get(),
                this.constructor
            );
        }
    }
}