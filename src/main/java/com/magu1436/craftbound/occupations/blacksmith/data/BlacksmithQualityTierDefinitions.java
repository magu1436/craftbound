package com.magu1436.craftbound.occupations.blacksmith.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.common.quality.QualityState;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class BlacksmithQualityTierDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = "blacksmith/quality";
    private static final ResourceLocation TIERS_ID = CraftboundUtilities.createResourceLocation("tiers");
    private static final int MAX_TIER_COUNT = QualityState.MAX_QUALITY - QualityState.MIN_QUALITY + 1;
    private static final List<QualityTierDefinition> DEFAULT_TIERS = List.of(
        new QualityTierDefinition("poor", 0, 19, "quality.craftbound.blacksmith.poor"),
        new QualityTierDefinition("low", 20, 39, "quality.craftbound.blacksmith.low"),
        new QualityTierDefinition("standard", 40, 59, "quality.craftbound.blacksmith.standard"),
        new QualityTierDefinition("high", 60, 79, "quality.craftbound.blacksmith.high"),
        new QualityTierDefinition("masterwork", 80, 100, "quality.craftbound.blacksmith.masterwork")
    );

    public static final BlacksmithQualityTierDefinitions INSTANCE = new BlacksmithQualityTierDefinitions();

    private static volatile List<QualityTierDefinition> current = DEFAULT_TIERS;

    private BlacksmithQualityTierDefinitions() {
        super(GSON, DIRECTORY);
    }

    public static List<QualityTierDefinition> all() {
        return current;
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> resources,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        JsonElement element = resources.get(TIERS_ID);
        if (element == null) {
            LOGGER.error("Missing blacksmith/quality/tiers.json; keeping the previous quality tier definitions");
            return;
        }

        try {
            List<QualityTierDefinition> parsed = parse(element);
            current = parsed;
            LOGGER.info("Loaded {} blacksmith quality tiers", parsed.size());
        } catch (RuntimeException exception) {
            LOGGER.error(
                "Invalid blacksmith/quality/tiers.json; keeping the previous quality tier definitions: {}",
                exception.getMessage()
            );
        }
    }

    static List<QualityTierDefinition> parse(JsonElement element) {
        if (!element.isJsonObject()) throw new JsonParseException("root must be an object");

        JsonObject root = element.getAsJsonObject();
        JsonArray tierElements = GsonHelper.getAsJsonArray(root, "tiers");
        if (tierElements.isEmpty()) throw new JsonParseException("tiers must not be empty");
        if (tierElements.size() > MAX_TIER_COUNT) {
            throw new JsonParseException("tier count must not exceed " + MAX_TIER_COUNT);
        }

        List<QualityTierDefinition> tiers = new ArrayList<>(tierElements.size());
        Set<String> ids = new HashSet<>();
        for (JsonElement tierElement : tierElements) {
            if (!tierElement.isJsonObject()) throw new JsonParseException("each tier must be an object");

            JsonObject tierJson = tierElement.getAsJsonObject();
            String id = GsonHelper.getAsString(tierJson, "id");
            int min = GsonHelper.getAsInt(tierJson, "min");
            int max = GsonHelper.getAsInt(tierJson, "max");
            String translationKey = GsonHelper.getAsString(tierJson, "translation_key");
            if (id.isBlank()) throw new JsonParseException("tier id must not be blank");
            if (!ids.add(id)) throw new JsonParseException("duplicate tier id: " + id);
            if (translationKey.isBlank()) {
                throw new JsonParseException("translation_key must not be blank for tier: " + id);
            }
            if (!QualityState.isValidQuality(min) || !QualityState.isValidQuality(max) || min > max) {
                throw new JsonParseException("invalid quality range for tier " + id + ": " + min + ".." + max);
            }
            tiers.add(new QualityTierDefinition(id, min, max, translationKey));
        }

        tiers.sort(Comparator.comparingInt(QualityTierDefinition::min));
        validateCompleteCoverage(tiers);
        return List.copyOf(tiers);
    }

    private static void validateCompleteCoverage(List<QualityTierDefinition> tiers) {
        int expectedMin = QualityState.MIN_QUALITY;
        for (QualityTierDefinition tier : tiers) {
            if (tier.min() != expectedMin) {
                throw new JsonParseException(
                    "quality ranges must be contiguous; expected " + expectedMin + " but found " + tier.min()
                );
            }
            expectedMin = tier.max() + 1;
        }
        if (expectedMin != QualityState.MAX_QUALITY + 1) {
            throw new JsonParseException("quality ranges must end at " + QualityState.MAX_QUALITY);
        }
    }
}
