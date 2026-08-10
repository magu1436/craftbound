package com.magu1436.craftbound.occupations.blacksmith.data;

public record BlacksmithSettings(
    int schemaVersion,
    int crucibleCapacityUnits,
    Network network
) {
    public record Network(
        int forgingLatencyCompensationTicks,
        int carvingPacketIntervalTicks,
        int carvingMaxCellsPerRequest,
        double workbenchInteractionDistance,
        int sessionHeartbeatTicks,
        int disconnectGraceTicks
    ) {}
}
