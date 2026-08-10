package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.capability.IAdventurerData;
import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkData;
import com.magu1436.craftbound.occupations.architect.capability.IArchitectData;
import com.magu1436.craftbound.occupations.blacksmith.capability.IBlacksmithData;
import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Craftboundがプレイヤーへ付与するCapabilityの定義。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class CraftboundCapabilities {

    public static final Capability<IAdventurerData> ADVENTURER_DATA =
        CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<ConstructionChunkData>
        CONSTRUCTION_CHUNK_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IArchitectData> ARCHITECT_DATA =
        CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<ExplorerDiscoveryData>
        EXPLORER_DISCOVERY_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});
    
    public static final Capability<IBlacksmithData>
        BLACKSMITH_CAPABILITY_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    private CraftboundCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IAdventurerData.class);
        event.register(ConstructionChunkData.class);
        event.register(IArchitectData.class);
        event.register(ExplorerDiscoveryData.class);
        event.register(IBlacksmithData.class);
    }
}
