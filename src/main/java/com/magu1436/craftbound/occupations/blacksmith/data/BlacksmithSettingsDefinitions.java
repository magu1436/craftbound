package com.magu1436.craftbound.occupations.blacksmith.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.common.CraftboundUtilities;
import com.mojang.logging.LogUtils;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class BlacksmithSettingsDefinitions extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final ResourceLocation SETTINGS_ID = CraftboundUtilities.createResourceLocation("settings");
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    public static final BlacksmithSettingsDefinitions INSTANCE = new BlacksmithSettingsDefinitions();

    private volatile BlacksmithSettings settings;

    private BlacksmithSettingsDefinitions() {
        super(GSON, "blacksmith");
    }

    public Optional<BlacksmithSettings> get() {
        return Optional.ofNullable(settings);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> resources,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        JsonElement element = resources.get(SETTINGS_ID);
        if (element == null) {
            settings = null;
            LOGGER.error("Missing blacksmith/settings.json; blacksmith sessions are disabled");
            return;
        }
        try {
            settings = parse(element);
            LOGGER.info("Loaded blacksmith settings");
        } catch (RuntimeException exception) {
            LOGGER.error("Invalid blacksmith/settings.json: {}", exception.getMessage());
        }
    }

    static BlacksmithSettings parse(JsonElement element) {
        if (!element.isJsonObject()) throw new JsonParseException("settings must be an object");
        JsonObject json = element.getAsJsonObject();
        int schema = GsonHelper.getAsInt(json, "schema_version");
        int capacity = GsonHelper.getAsInt(json, "crucible_capacity_units");
        JsonObject network = GsonHelper.getAsJsonObject(json, "network");
        BlacksmithSettings.Network parsedNetwork = new BlacksmithSettings.Network(
            GsonHelper.getAsInt(network, "forging_latency_compensation_ticks"),
            GsonHelper.getAsInt(network, "carving_packet_interval_ticks"),
            GsonHelper.getAsInt(network, "carving_max_cells_per_request"),
            GsonHelper.getAsDouble(network, "workbench_interaction_distance"),
            GsonHelper.getAsInt(network, "session_heartbeat_ticks"),
            GsonHelper.getAsInt(network, "disconnect_grace_ticks")
        );
        if (schema != SUPPORTED_SCHEMA_VERSION || capacity < 1
            || parsedNetwork.forgingLatencyCompensationTicks() < 0
            || parsedNetwork.carvingPacketIntervalTicks() < 1
            || parsedNetwork.carvingMaxCellsPerRequest() < 1
            || !Double.isFinite(parsedNetwork.workbenchInteractionDistance())
            || parsedNetwork.workbenchInteractionDistance() <= 0.0D
            || parsedNetwork.sessionHeartbeatTicks() < 1
            || parsedNetwork.disconnectGraceTicks() < 1) {
            throw new JsonParseException("settings contain an unsupported or non-positive value");
        }
        return new BlacksmithSettings(schema, capacity, parsedNetwork);
    }
}
