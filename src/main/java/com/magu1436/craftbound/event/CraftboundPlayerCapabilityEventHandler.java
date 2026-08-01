package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.common.capability.PlayerCapabilityData;
import com.magu1436.craftbound.common.capability.PlayerCapabilityProvider;
import com.magu1436.craftbound.occupations.adventurer.capability.AdventurerData;
import com.magu1436.craftbound.occupations.architect.capability.ArchitectData;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * プレイヤーCapabilityの付与と死亡時コピーを処理する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundPlayerCapabilityEventHandler {
    private static final String ADVENTURER_DATA_ID = "adventurer_data";
    private static final String ARCHITECT_DATA_ID = "architect_data";

    private CraftboundPlayerCapabilityEventHandler() {
    }

    @SubscribeEvent
    public static void attachPlayerCapabilities(
        AttachCapabilitiesEvent<Entity> event
    ) {
        if (!(event.getObject() instanceof Player)) {
            return;
        }

        PlayerCapabilityProvider<?> adventurerProvider =
            createAdventurerDataProvider();

        event.addCapability(
            CraftboundUtilities.createResourceLocation(
                ADVENTURER_DATA_ID
            ),
            adventurerProvider
        );
        event.addListener(adventurerProvider::invalidate);

        PlayerCapabilityProvider<?> architectProvider =
            createArchitectDataProvider();
        event.addCapability(
            CraftboundUtilities.createResourceLocation(ARCHITECT_DATA_ID),
            architectProvider
        );
        event.addListener(architectProvider::invalidate);
    }

    @SubscribeEvent
    public static void copyPlayerCapabilities(
        PlayerEvent.Clone event
    ) {
        if (!event.isWasDeath()) {
            return;
        }

        Player original = event.getOriginal();
        original.reviveCaps();

        try {
            copyOnDeath(
                original,
                event.getEntity(),
                CraftboundCapabilities.ADVENTURER_DATA
            );
            copyOnDeath(
                original,
                event.getEntity(),
                CraftboundCapabilities.ARCHITECT_DATA
            );
        } finally {
            original.invalidateCaps();
        }
    }

    private static PlayerCapabilityProvider<?> createAdventurerDataProvider() {
        return new PlayerCapabilityProvider<>(
            CraftboundCapabilities.ADVENTURER_DATA,
            AdventurerData::new
        );
    }

    private static PlayerCapabilityProvider<?> createArchitectDataProvider() {
        return new PlayerCapabilityProvider<>(
            CraftboundCapabilities.ARCHITECT_DATA,
            ArchitectData::new
        );
    }

    private static <T extends PlayerCapabilityData<T>> void copyOnDeath(
        Player original,
        Player replacement,
        Capability<T> capability
    ) {
        original.getCapability(capability).ifPresent(
            originalData -> replacement
                .getCapability(capability)
                .ifPresent(
                    replacementData ->
                        replacementData.copyOnDeathFrom(originalData)
                )
        );
    }
}
