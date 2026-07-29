package com.magu1436.craftbound.occupations.adventurer.experience;

import java.math.BigDecimal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mob経験値JSONを検証して定義へ変換する。
 */
final class MobExperienceDefinitionParser {
    private static final String ENTITY_KEY = "entity";
    private static final String CATEGORY_KEY = "category";
    private static final String EXPERIENCE_KEY = "experience";

    private MobExperienceDefinitionParser() {
    }

    public static MobExperienceDefinition parse(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException(
                "root element must be an object"
            );
        }

        JsonObject object = root.getAsJsonObject();
        ResourceLocation entityId = parseEntityId(object);
        EntityType<?> entityType =
            ForgeRegistries.ENTITY_TYPES.getValue(entityId);

        if (entityType == null) {
            throw new JsonParseException(
                "entity does not exist: " + entityId
            );
        }
        if (
            entityType == EntityType.PLAYER
                || !LivingEntity.class.isAssignableFrom(
                    entityType.getBaseClass()
                )
        ) {
            throw new JsonParseException(
                "entity must be a living non-player entity: "
                    + entityId
            );
        }

        String categoryName = getRequiredString(
            object,
            CATEGORY_KEY
        );
        MobExperienceCategory category =
            MobExperienceCategory
                .fromSerializedName(categoryName)
                .orElseThrow(() ->
                    new JsonParseException(
                        "unknown category: " + categoryName
                    )
                );

        int experience = getRequiredNonNegativeInt(
            object,
            EXPERIENCE_KEY
        );

        return new MobExperienceDefinition(
            entityId,
            entityType,
            category,
            experience
        );
    }

    private static ResourceLocation parseEntityId(
        JsonObject object
    ) {
        String value = getRequiredString(object, ENTITY_KEY);
        ResourceLocation entityId = ResourceLocation.tryParse(value);

        if (entityId == null) {
            throw new JsonParseException(
                "invalid entity id: " + value
            );
        }

        return entityId;
    }

    private static String getRequiredString(
        JsonObject object,
        String key
    ) {
        JsonElement element = object.get(key);

        if (
            element == null
                || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isString()
        ) {
            throw new JsonParseException(
                key + " must be a string"
            );
        }

        return element.getAsString();
    }

    private static int getRequiredNonNegativeInt(
        JsonObject object,
        String key
    ) {
        JsonElement element = object.get(key);

        if (
            element == null
                || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive().isNumber()
        ) {
            throw new JsonParseException(
                key + " must be an integer"
            );
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        BigDecimal value;

        try {
            value = primitive.getAsBigDecimal();
        } catch (NumberFormatException exception) {
            throw new JsonParseException(
                key + " must be an integer",
                exception
            );
        }

        int integerValue;

        try {
            integerValue = value.intValueExact();
        } catch (ArithmeticException exception) {
            throw new JsonParseException(
                key + " must be a 32-bit integer",
                exception
            );
        }

        if (integerValue < 0) {
            throw new JsonParseException(
                key + " must be greater than or equal to 0"
            );
        }

        return integerValue;
    }
}
