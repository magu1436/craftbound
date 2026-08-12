package com.magu1436.craftbound.occupations.foodproducer.quality;

import java.util.Arrays;

public enum FoodQuality {
    SPOILED(0),
    LOW(1),
    STANDARD(2),
    HIGH(3);

    private final int value;

    FoodQuality(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static FoodQuality fromValue(int value) {
        return Arrays.stream(values())
                .filter(quality -> quality.value == value)
                .findFirst()
                .orElse(STANDARD);
    }
}
