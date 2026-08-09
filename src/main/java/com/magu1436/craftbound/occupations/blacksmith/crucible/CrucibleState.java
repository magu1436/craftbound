package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record CrucibleState(
    int version,
    ResourceLocation metalId,
    int amount,
    long heatingTicks,
    CrucibleProcessState processState
) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_CAPACITY = 16;

    public CrucibleState {
        Objects.requireNonNull(processState, "processState");
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException(
                "unsupported version: " + version
            );
        }
        if (!isValid(metalId, amount, heatingTicks, processState)) {
            throw new IllegalArgumentException(
                "state fields do not satisfy the crucible invariants"
            );
        }
    }

    public static CrucibleState empty() {
        return new CrucibleState(
            CURRENT_VERSION,
            null,
            0,
            0L,
            CrucibleProcessState.EMPTY
        );
    }

    private static boolean isValid(
        ResourceLocation metalId,
        int amount,
        long heatingTicks,
        CrucibleProcessState processState
    ) {
        return switch (processState) {
            case EMPTY -> metalId == null
                && amount == 0
                && heatingTicks == 0L;
            case UNHEATED -> metalId != null
                && amount > 0
                && amount <= MAX_CAPACITY
                && heatingTicks == 0L;
            case HEATED -> metalId != null
                && amount > 0
                && amount <= MAX_CAPACITY
                && heatingTicks > 0L;
        };
    }
}
