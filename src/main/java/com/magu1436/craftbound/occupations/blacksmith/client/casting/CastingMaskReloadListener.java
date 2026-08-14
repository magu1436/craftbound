package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

public final class CastingMaskReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "blacksmith/casting_masks";
    private static final int SCHEMA_VERSION = 1;
    private static final int MASK_SIZE = 16;

    public CastingMaskReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> input,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        Map<ResourceLocation, CastingMaskDefinition> definitionsByMold =
            new HashMap<>();
        Map<ResourceLocation, ResourceLocation> sourceByMold = new HashMap<>();
        Set<ResourceLocation> ambiguousMolds = new HashSet<>();

        input.forEach((fileId, element) -> {
            try {
                CastingMaskDefinition definition = parse(fileId, element);
                ResourceLocation moldId = definition.moldItemId();
                ResourceLocation previousSource = sourceByMold.putIfAbsent(
                    moldId,
                    fileId
                );
                if (ambiguousMolds.contains(moldId) || previousSource != null) {
                    ambiguousMolds.add(moldId);
                    definitionsByMold.remove(moldId);
                    LOGGER.warn(
                        "Skipping duplicate casting masks {} and {} for mold {}",
                        previousSource,
                        fileId,
                        moldId
                    );
                    return;
                }
                definitionsByMold.put(moldId, definition);
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Skipping invalid casting mask {} for mold {}: {}",
                    fileId,
                    moldForLog(element),
                    exception.getMessage()
                );
            }
        });

        CastingMaskRegistry.replace(definitionsByMold);
        LOGGER.info("Loaded {} casting masks", definitionsByMold.size());
    }

    static CastingMaskDefinition parse(
        ResourceLocation definitionId,
        JsonElement element
    ) {
        if (!element.isJsonObject()) {
            throw new JsonParseException("definition must be a JSON object");
        }
        JsonObject json = element.getAsJsonObject();
        require(
            GsonHelper.getAsInt(json, "schema_version") == SCHEMA_VERSION,
            "schema_version must be " + SCHEMA_VERSION
        );
        ResourceLocation moldItemId = parseNamespacedId(
            GsonHelper.getAsString(json, "mold"),
            "mold"
        );
        int width = GsonHelper.getAsInt(json, "width");
        int height = GsonHelper.getAsInt(json, "height");
        require(width == MASK_SIZE, "width must be " + MASK_SIZE);
        require(height == MASK_SIZE, "height must be " + MASK_SIZE);

        JsonArray pixels = GsonHelper.getAsJsonArray(json, "pixels");
        require(pixels.size() == MASK_SIZE, "pixels must contain 16 rows");
        List<String> rows = parseRows(pixels);
        CastingMaskGeometry geometry = createGeometry(rows);
        require(
            !geometry.cavitySpans().isEmpty(),
            "pixels must contain at least one C cell"
        );
        return new CastingMaskDefinition(
            definitionId,
            moldItemId,
            width,
            height,
            geometry
        );
    }

    private static List<String> parseRows(JsonArray pixels) {
        List<String> rows = new ArrayList<>(MASK_SIZE);
        for (int row = 0; row < pixels.size(); row++) {
            JsonElement element = pixels.get(row);
            require(
                element.isJsonPrimitive()
                    && element.getAsJsonPrimitive().isString(),
                "pixels row " + row + " must be a string"
            );
            String value = element.getAsString();
            require(
                value.length() == MASK_SIZE,
                "pixels row " + row + " must contain 16 characters"
            );
            for (int column = 0; column < value.length(); column++) {
                char symbol = value.charAt(column);
                require(
                    symbol == '.' || symbol == 'R' || symbol == 'C',
                    "pixels row " + row + " contains unsupported symbol `"
                        + symbol + "`"
                );
            }
            rows.add(value);
        }
        return List.copyOf(rows);
    }

    private static CastingMaskGeometry createGeometry(List<String> rows) {
        return new CastingMaskGeometry(
            collectSpans(rows, 'R'),
            collectSpans(rows, 'C')
        );
    }

    private static List<MaskSpan> collectSpans(List<String> rows, char symbol) {
        List<MaskSpan> spans = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            String pixels = rows.get(row);
            int column = 0;
            while (column < MASK_SIZE) {
                if (pixels.charAt(column) != symbol) {
                    column++;
                    continue;
                }
                int startColumn = column;
                while (column < MASK_SIZE && pixels.charAt(column) == symbol) {
                    column++;
                }
                spans.add(new MaskSpan(row, startColumn, column));
            }
        }
        return List.copyOf(spans);
    }

    private static ResourceLocation parseNamespacedId(
        String value,
        String field
    ) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        require(
            id != null && value.contains(":"),
            field + " must be a namespaced resource id"
        );
        return id;
    }

    private static String moldForLog(JsonElement element) {
        if (!element.isJsonObject()) {
            return "<unavailable>";
        }
        JsonElement mold = element.getAsJsonObject().get("mold");
        return mold != null && mold.isJsonPrimitive()
            ? mold.getAsString()
            : "<unavailable>";
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new JsonParseException(message);
        }
    }
}
