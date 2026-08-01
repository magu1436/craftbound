package com.magu1436.craftbound.occupations.architect.capability;

import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;

import com.magu1436.craftbound.registry.CraftboundCapabilities;

public final class ArchitectDataAccess {

    private ArchitectDataAccess() {
    }

    public static Optional<IArchitectData> get(ServerPlayer player) {
        return player
            .getCapability(CraftboundCapabilities.ARCHITECT_DATA)
            .resolve();
    }
}
