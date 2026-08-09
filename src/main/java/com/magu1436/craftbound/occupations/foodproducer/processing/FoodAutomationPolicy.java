package com.magu1436.craftbound.occupations.foodproducer.processing;

/** FoodProducer加工を外部自動化へ公開する範囲。 */
public enum FoodAutomationPolicy {
    MANUAL_ONLY("manual_only"),
    CREATE_LOW_QUALITY("create_low_quality");

    private final String serializedName;

    FoodAutomationPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static FoodAutomationPolicy fromName(String name) {
        for (FoodAutomationPolicy value : values()) {
            if (value.serializedName.equals(name)) return value;
        }
        throw new IllegalArgumentException("unknown automation policy: " + name);
    }
}
