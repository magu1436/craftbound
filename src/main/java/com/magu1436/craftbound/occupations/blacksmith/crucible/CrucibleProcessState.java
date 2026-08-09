package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Locale;
import java.util.Optional;

public enum CrucibleProcessState {
    EMPTY,
    UNHEATED,
    HEATED;

    public String serializedName() {
        return name();
    }

    public static Optional<CrucibleProcessState> fromSerializedName(
        String name
    ) {
        if (name == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
