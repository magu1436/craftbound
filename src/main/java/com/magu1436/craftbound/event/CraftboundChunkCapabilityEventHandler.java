package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.architect.capability.PlayerPlacedBlockProvider;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * チャンクへCraftbound固有のCapabilityを付与する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundChunkCapabilityEventHandler {
    private static final String PLAYER_PLACED_BLOCKS_ID =
        "player_placed_blocks";

    private CraftboundChunkCapabilityEventHandler() {
    }

    @SubscribeEvent
    public static void attachChunkCapabilities(
        AttachCapabilitiesEvent<LevelChunk> event
    ) {
        PlayerPlacedBlockProvider provider =
            new PlayerPlacedBlockProvider();

        event.addCapability(
            CraftboundUtilities.createResourceLocation(
                PLAYER_PLACED_BLOCKS_ID
            ),
            provider
        );
        event.addListener(provider::invalidate);
    }
}
