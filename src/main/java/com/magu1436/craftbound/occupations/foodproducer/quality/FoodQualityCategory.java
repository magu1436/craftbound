package com.magu1436.craftbound.occupations.foodproducer.quality;

public enum FoodQualityCategory {
    MATERIAL(120L * 60L * 20L),
    DISH(20L * 60L * 20L),
    PRESERVED_FOOD(240L * 60L * 20L);

    private final long stageDurationTicks;

    FoodQualityCategory(long stageDurationTicks) {
        this.stageDurationTicks = stageDurationTicks;
    }

    public long stageDurationTicks() {
        return stageDurationTicks;
    }
}
