package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Objects;

public record CrucibleInsertResult(
    int acceptedItemCount,
    int addedAmount,
    CrucibleState state,
    CrucibleInsertFailure failure
) {
    public CrucibleInsertResult {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(failure, "failure");
        if (acceptedItemCount < 0 || addedAmount < 0) {
            throw new IllegalArgumentException("accepted values cannot be negative");
        }
        if ((failure == CrucibleInsertFailure.NONE)
            != (acceptedItemCount > 0)) {
            throw new IllegalArgumentException(
                "success must accept at least one item"
            );
        }
    }

    public boolean succeeded() {
        return failure == CrucibleInsertFailure.NONE;
    }

    public static CrucibleInsertResult success(
        int acceptedItemCount,
        int addedAmount,
        CrucibleState state
    ) {
        return new CrucibleInsertResult(
            acceptedItemCount,
            addedAmount,
            state,
            CrucibleInsertFailure.NONE
        );
    }

    public static CrucibleInsertResult failure(
        CrucibleInsertFailure failure,
        CrucibleState state
    ) {
        return new CrucibleInsertResult(0, 0, state, failure);
    }
}
