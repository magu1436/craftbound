package com.magu1436.craftbound.occupations.foodproducer.quality;

public enum FoodQualityCategory {
    MATERIAL(140L * 60L * 20L),
    DISH(70L * 60L * 20L),
    PRESERVED_FOOD(280L * 60L * 20L),
    REGIONAL_INGREDIENT(24L * 60L * 60L * 20L);

    private final long stageDurationTicks;

    FoodQualityCategory(long stageDurationTicks) {
        this.stageDurationTicks = stageDurationTicks;
    }

    public long stageDurationTicks() {
        return stageDurationTicks;
    }
}
