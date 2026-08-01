package com.magu1436.craftbound.occupations.foodproducer.processing;

public enum FoodProcessingOperation {
    CUT("cut", 5 * 20),
    GRIND("grind", 10 * 20),
    PRESERVE("preserve", 60 * 20),
    MIX("mix", 10 * 20),
    HEAT("heat", 30 * 20);

    private final String serializedName;
    private final int baseTicks;

    FoodProcessingOperation(String serializedName, int baseTicks) {
        this.serializedName = serializedName;
        this.baseTicks = baseTicks;
    }

    public String serializedName() {
        return serializedName;
    }

    public int baseTicks() {
        return baseTicks;
    }
}
