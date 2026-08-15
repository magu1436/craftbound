package com.magu1436.craftbound.occupations.blacksmith.carving.definition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

final class CarvingJson {
    private CarvingJson() {}
    static ResourceLocation id(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        ResourceLocation id = ResourceLocation.tryParse(value);
        require(id != null && value.contains(":"), field + " must be a namespaced id");
        return id;
    }
    static double positive(JsonObject json, String field) {
        double value = GsonHelper.getAsDouble(json, field);
        require(Double.isFinite(value) && value > 0.0D, field + " must be positive");
        return value;
    }
    static double ratio(JsonObject json, String field) {
        double value = GsonHelper.getAsDouble(json, field);
        require(Double.isFinite(value) && value >= 0.0D && value <= 1.0D,
            field + " must be between 0 and 1");
        return value;
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new JsonParseException(message);
    }
}
