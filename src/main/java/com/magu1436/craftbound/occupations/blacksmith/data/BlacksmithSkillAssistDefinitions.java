package com.magu1436.craftbound.occupations.blacksmith.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinition.*;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.GaugeDirection;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public final class BlacksmithSkillAssistDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation DEFINITION_ID = CraftboundUtilities.createResourceLocation("skill_assists");

    public static final BlacksmithSkillAssistDefinitions INSTANCE = new BlacksmithSkillAssistDefinitions();

    private volatile BlacksmithSkillAssistDefinition definition;

    private BlacksmithSkillAssistDefinitions() {
        super(GSON, "blacksmith");
    }

    public Optional<BlacksmithSkillAssistDefinition> get() {
        return Optional.ofNullable(definition);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        JsonElement element = resources.get(DEFINITION_ID);
        if (element == null) {
            definition = null;
            LOGGER.error("Missing blacksmith/skill_assists.json; forging sessions are disabled");
            return;
        }
        try {
            definition = parse(element);
            LOGGER.info("Loaded blacksmith skill assists");
        } catch (RuntimeException exception) {
            LOGGER.error("Invalid blacksmith/skill_assists.json: {}", exception.getMessage());
        }
    }

    static BlacksmithSkillAssistDefinition parse(JsonElement element) {
        if (!element.isJsonObject()) throw new JsonParseException("definition must be an object");
        JsonObject json = element.getAsJsonObject();
        int schema = GsonHelper.getAsInt(json, "schema_version");
        if (schema != 1) throw new JsonParseException("unsupported schema_version");

        JsonObject gaugeJson = GsonHelper.getAsJsonObject(json, "forging_gauge");
        double min = GsonHelper.getAsDouble(gaugeJson, "min");
        double max = GsonHelper.getAsDouble(gaugeJson, "max");
        int cycle = GsonHelper.getAsInt(gaugeJson, "cycle_ticks");
        if (!"triangle".equals(GsonHelper.getAsString(gaugeJson, "waveform"))) {
            throw new JsonParseException("only triangle forging gauges are supported");
        }
        GaugeDirection direction = GaugeDirection.fromSerializedName(
            GsonHelper.getAsString(gaugeJson, "initial_direction")
        ).orElseThrow(() -> new JsonParseException("unknown initial_direction"));
        Gauge gauge = new Gauge(min, max, cycle,
            GsonHelper.getAsDouble(gaugeJson, "initial_value"), direction,
            GsonHelper.getAsInt(gaugeJson, "base_mark_interval"));
        validateGauge(gauge);

        List<PrecisionScaleLevel> precision = parsePrecision(
            GsonHelper.getAsJsonArray(GsonHelper.getAsJsonObject(json, "precision_scale"), "levels"),
            min, max
        );
        JsonObject force = GsonHelper.getAsJsonObject(json, "force_reading");
        JsonObject reference = GsonHelper.getAsJsonObject(json, "strike_reference");
        int forceEnabledLevel = GsonHelper.getAsInt(force, "enabled_at_level");
        int referenceEnabledLevel = GsonHelper.getAsInt(reference, "enabled_at_level");
        int referenceMarkers = GsonHelper.getAsInt(reference, "max_markers");
        boolean referenceEnabled = referenceMarkers == 1
            && "session".equals(GsonHelper.getAsString(reference, "persistence"));
        if (forceEnabledLevel < 1 || referenceEnabledLevel < 1 || !referenceEnabled) {
            throw new JsonParseException("invalid force_reading or strike_reference section");
        }

        JsonObject instinct = GsonHelper.getAsJsonObject(json, "smithing_instinct");
        List<SmithingInstinctLevel> instinctLevels = parseInstinct(
            GsonHelper.getAsJsonArray(instinct, "levels")
        );
        JsonObject audio = GsonHelper.getAsJsonObject(instinct, "audio");
        ResourceLocation soundId = parseId(GsonHelper.getAsString(audio, "sound"));
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound == null) throw new JsonParseException("unknown sound `" + soundId + "`");
        JsonObject pitch = GsonHelper.getAsJsonObject(audio, "pitch");
        InstinctAudio instinctAudio = new InstinctAudio(soundId,
            GsonHelper.getAsFloat(audio, "volume"), GsonHelper.getAsInt(audio, "cooldown_ticks"),
            GsonHelper.getAsFloat(pitch, "caution"), GsonHelper.getAsFloat(pitch, "danger"),
            GsonHelper.getAsFloat(pitch, "critical"));
        validateAudio(instinctAudio);

        validateFutureSections(json);
        return new BlacksmithSkillAssistDefinition(schema, gauge, precision,
            forceEnabledLevel, GsonHelper.getAsBoolean(force, "show_current_value"),
            referenceEnabledLevel, referenceEnabled,
            instinctLevels, instinctAudio);
    }

    private static void validateGauge(Gauge gauge) {
        double range = gauge.max() - gauge.min();
        if (!Double.isFinite(gauge.min()) || !Double.isFinite(gauge.max()) || gauge.min() >= gauge.max()
            || gauge.cycleTicks() < 2 || !Double.isFinite(gauge.initialValue())
            || gauge.initialValue() < gauge.min() || gauge.initialValue() > gauge.max()
            || gauge.baseMarkInterval() < 1 || range % gauge.baseMarkInterval() != 0.0D) {
            throw new JsonParseException("invalid forging_gauge");
        }
    }

    private static List<PrecisionScaleLevel> parsePrecision(JsonArray array, double min, double max) {
        List<PrecisionScaleLevel> levels = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int previousLevel = 0;
        int previousInterval = Integer.MAX_VALUE;
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            int level = GsonHelper.getAsInt(entry, "level");
            int interval = GsonHelper.getAsInt(entry, "mark_interval");
            if (level <= previousLevel || !seen.add(level) || interval < 1 || interval >= previousInterval
                || (max - min) % interval != 0.0D) {
                throw new JsonParseException("invalid precision_scale level");
            }
            levels.add(new PrecisionScaleLevel(level, interval));
            previousLevel = level;
            previousInterval = interval;
        }
        return List.copyOf(levels);
    }

    private static List<SmithingInstinctLevel> parseInstinct(JsonArray array) {
        List<SmithingInstinctLevel> levels = new ArrayList<>();
        int previousLevel = 0;
        int previous = 0;
        for (JsonElement element : array) {
            JsonObject entry = element.getAsJsonObject();
            int level = GsonHelper.getAsInt(entry, "level");
            int remaining = GsonHelper.getAsInt(entry, "forging_remaining_hits");
            if (level <= previousLevel || remaining < 1 || remaining <= previous) {
                throw new JsonParseException("invalid smithing_instinct level");
            }
            levels.add(new SmithingInstinctLevel(level, remaining));
            previousLevel = level;
            previous = remaining;
        }
        return List.copyOf(levels);
    }

    private static void validateAudio(InstinctAudio audio) {
        if (audio.volume() < 0.0F || audio.cooldownTicks() < 1
            || !positiveFinite(audio.cautionPitch()) || !positiveFinite(audio.dangerPitch())
            || !positiveFinite(audio.criticalPitch())) {
            throw new JsonParseException("invalid smithing_instinct audio");
        }
    }

    private static void validateFutureSections(JsonObject json) {
        if (!json.has("precision_shaping") || !json.has("tool_preservation")
            || !json.has("material_insight") || !json.has("quality_appraisal")) {
            throw new JsonParseException("missing future-compatible skill assist section");
        }
        JsonObject shaping = GsonHelper.getAsJsonObject(json, "precision_shaping");
        int previousGrid = GsonHelper.getAsInt(shaping, "base_grid_size");
        double previousRadius = GsonHelper.getAsDouble(shaping, "base_brush_radius");
        if (previousGrid < 1 || !positiveFinite(previousRadius)) {
            throw new JsonParseException("invalid precision_shaping base values");
        }
        for (JsonElement element : GsonHelper.getAsJsonArray(shaping, "levels")) {
            JsonObject entry = element.getAsJsonObject();
            int grid = GsonHelper.getAsInt(entry, "grid_size");
            double radius = GsonHelper.getAsDouble(entry, "brush_radius");
            if (grid <= previousGrid || !positiveFinite(radius) || radius >= previousRadius) {
                throw new JsonParseException("invalid precision_shaping level");
            }
            previousGrid = grid;
            previousRadius = radius;
        }

        double previousChance = -1.0D;
        int previousLevel = 0;
        for (JsonElement element : GsonHelper.getAsJsonArray(
            GsonHelper.getAsJsonObject(json, "tool_preservation"), "levels")) {
            JsonObject entry = element.getAsJsonObject();
            int level = GsonHelper.getAsInt(entry, "level");
            double chance = GsonHelper.getAsDouble(entry, "prevent_damage_chance");
            if (level <= previousLevel || !Double.isFinite(chance)
                || chance < 0.0D || chance > 1.0D || chance < previousChance) {
                throw new JsonParseException("invalid tool_preservation level");
            }
            previousLevel = level;
            previousChance = chance;
        }
    }

    private static boolean positiveFinite(float value) {
        return Float.isFinite(value) && value > 0.0F;
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0D;
    }

    private static ResourceLocation parseId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) throw new JsonParseException("invalid resource id");
        return id;
    }
}
