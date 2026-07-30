package com.magu1436.craftbound.occupations.adventurer.experience;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Mob経験値JSONを検証して定義へ変換する。
 */
final class MobExperienceDefinitionParser {
    private static final String ENTRIES_KEY = "entries";
    private static final String ENTITY_KEY = "entity";
    private static final String CATEGORY_KEY = "category";
    private static final String EXPERIENCE_KEY = "experience";

    private MobExperienceDefinitionParser() {
    }

    public static ParseResult parse(JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new JsonParseException(
                "root element must be an object"
            );
        }

        JsonObject object = root.getAsJsonObject();
        JsonElement entriesElement = object.get(ENTRIES_KEY);

        if (
            entriesElement == null
                || !entriesElement.isJsonArray()
        ) {
            throw new JsonParseException(
                ENTRIES_KEY + " must be an array"
            );
        }

        List<ParsedDefinition> definitions = new ArrayList<>();
        List<EntryError> errors = new ArrayList<>();
        int entryIndex = 0;

        for (JsonElement entry : entriesElement.getAsJsonArray()) {
            try {
                definitions.add(
                    new ParsedDefinition(
                        entryIndex,
                        parseEntry(entry)
                    )
                );
            } catch (
                JsonParseException
                    | IllegalArgumentException exception
            ) {
                errors.add(
                    new EntryError(
                        entryIndex,
                        exception.getMessage()
                    )
                );
            }

            entryIndex++;
        }

        return new ParseResult(
            List.copyOf(definitions),
            List.copyOf(errors)
        );
    }

    private static MobExperienceDefinition parseEntry(
        JsonElement entry
    ) {
        if (entry == null || !entry.isJsonObject()) {
            throw new JsonParseException(
                "entry must be an object"
            );
        }

        JsonObject object = entry.getAsJsonObject();
        ResourceLocation entityId = parseEntityId(object);
        EntityType<?> entityType =
            ForgeRegistries.ENTITY_TYPES.getValue(entityId);

        if (entityType == null) {
            throw new JsonParseException(
                "entity does not exist: " + entityId
            );
        }

        // Forge 1.20.1 returns Entity.class from getBaseClass() for vanilla
        // types. LivingDeathEvent guarantees the awarded entity is living.
        if (entityType == EntityType.PLAYER) {
            throw new JsonParseException(
                "entity must not be a player: "
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

    record ParseResult(
        List<ParsedDefinition> definitions,
        List<EntryError> errors
    ) {
    }

    record ParsedDefinition(
        int entryIndex,
        MobExperienceDefinition definition
    ) {
    }

    record EntryError(
        int entryIndex,
        String message
    ) {
    }
}
