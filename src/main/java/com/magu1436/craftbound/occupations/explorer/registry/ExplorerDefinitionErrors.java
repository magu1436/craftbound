package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;

/** データ定義のエラーを読み込み単位で集約する。 */
public final class ExplorerDefinitionErrors {
    private final Map<ResourceLocation, List<String>> errors =
        new LinkedHashMap<>();

    public void add(ResourceLocation definitionId, String message) {
        errors.computeIfAbsent(
            Objects.requireNonNull(definitionId),
            ignored -> new ArrayList<>()
        ).add(Objects.requireNonNull(message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int definitionCount() {
        return errors.size();
    }

    public int messageCount() {
        return errors.values().stream().mapToInt(List::size).sum();
    }

    public Map<ResourceLocation, List<String>> entries() {
        Map<ResourceLocation, List<String>> copy = new LinkedHashMap<>();
        errors.forEach((id, messages) -> copy.put(id, List.copyOf(messages)));
        return Map.copyOf(copy);
    }

    public void logAll(Logger logger) {
        if (!hasErrors()) {
            return;
        }
        logger.error(
            "Found errors in {} explorer discovery definitions",
            definitionCount()
        );
        errors.forEach((id, messages) -> messages.forEach(message ->
            logger.error("[{}] {}", id, message)
        ));
    }
}

record ExplorerParseResult<T>(T value) {
    static <T> ExplorerParseResult<T> success(T value) {
        return new ExplorerParseResult<>(Objects.requireNonNull(value));
    }

    static <T> ExplorerParseResult<T> failure() {
        return new ExplorerParseResult<>(null);
    }

    boolean isSuccess() {
        return value != null;
    }
}

final class ExplorerJsonValidation {
    static final int FORMAT_VERSION = 1;

    private ExplorerJsonValidation() {
    }

    static boolean validateHeader(
        ResourceLocation id,
        JsonObject json,
        Set<String> allowedFields,
        ExplorerDefinitionErrors errors
    ) {
        boolean valid = true;
        for (String field : json.keySet()) {
            if (!allowedFields.contains(field)) {
                errors.add(id, "Unknown field: " + field);
                valid = false;
            }
        }
        Integer version = requiredInt(id, json, "format_version", errors);
        if (version == null || version != FORMAT_VERSION) {
            errors.add(id, "Unsupported format_version");
            valid = false;
        }
        return valid;
    }

    static Integer requiredInt(
        ResourceLocation id,
        JsonObject json,
        String field,
        ExplorerDefinitionErrors errors
    ) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive()
            || !value.getAsJsonPrimitive().isNumber()) {
            errors.add(id, field + " must be an integer");
            return null;
        }
        try {
            Number number = value.getAsNumber();
            int integer = number.intValue();
            if (number.doubleValue() != integer) {
                throw new NumberFormatException();
            }
            return integer;
        } catch (NumberFormatException exception) {
            errors.add(id, field + " must be an integer");
            return null;
        }
    }

    static Integer optionalInt(
        ResourceLocation id,
        JsonObject json,
        String field,
        int defaultValue,
        ExplorerDefinitionErrors errors
    ) {
        return json.has(field)
            ? requiredInt(id, json, field, errors)
            : defaultValue;
    }

    static String requiredString(
        ResourceLocation id,
        JsonObject json,
        String field,
        ExplorerDefinitionErrors errors
    ) {
        JsonElement value = json.get(field);
        if (value == null || !value.isJsonPrimitive()
            || !value.getAsJsonPrimitive().isString()) {
            errors.add(id, field + " must be a string");
            return null;
        }
        String text = value.getAsString();
        if (text.isBlank()) {
            errors.add(id, field + " must not be blank");
            return null;
        }
        return text;
    }

    static String optionalString(
        ResourceLocation id,
        JsonObject json,
        String field,
        ExplorerDefinitionErrors errors
    ) {
        return json.has(field)
            ? requiredString(id, json, field, errors)
            : null;
    }

    static ResourceLocation requiredId(
        ResourceLocation id,
        JsonObject json,
        String field,
        ExplorerDefinitionErrors errors
    ) {
        String text = requiredString(id, json, field, errors);
        ResourceLocation result = text == null
            ? null
            : ResourceLocation.tryParse(text);
        if (text != null && result == null) {
            errors.add(id, field + " is not a valid resource location");
        }
        return result;
    }

    static boolean optionalBoolean(
        ResourceLocation id,
        JsonObject json,
        String field,
        boolean defaultValue,
        ExplorerDefinitionErrors errors
    ) {
        if (!json.has(field)) {
            return defaultValue;
        }
        JsonElement value = json.get(field);
        if (!value.isJsonPrimitive()
            || !value.getAsJsonPrimitive().isBoolean()) {
            errors.add(id, field + " must be a boolean");
            return defaultValue;
        }
        return value.getAsBoolean();
    }
}
