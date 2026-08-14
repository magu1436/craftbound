package com.magu1436.craftbound.occupations.blacksmith.forging.state;

import java.util.Locale;
import java.util.Optional;

public enum GaugeDirection {
    UP,
    DOWN;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<GaugeDirection> fromSerializedName(String value) {
        if (value == null) return Optional.empty();
        return switch (value) {
            case "up" -> Optional.of(UP);
            case "down" -> Optional.of(DOWN);
            default -> Optional.empty();
        };
    }
}
