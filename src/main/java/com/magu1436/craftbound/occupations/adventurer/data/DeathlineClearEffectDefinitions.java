package com.magu1436.craftbound.occupations.adventurer.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

/**
 * 死線踏破の発動時に解除する状態異常のデータ定義。
 */
public final class DeathlineClearEffectDefinitions
    extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY =
        "craftbound/adventurer/deathline_clear_effects";
    private static final String EFFECT_KEY = "effect";

    public static final DeathlineClearEffectDefinitions INSTANCE =
        new DeathlineClearEffectDefinitions();

    private volatile Set<ResourceLocation> effectIds = Set.of();

    private DeathlineClearEffectDefinitions() {
        super(GSON, DIRECTORY);
    }

    /**
     * 指定した状態異常が解除対象かを返す。
     *
     * @param effect 状態異常
     * @return 解除対象の場合は {@code true}
     */
    public boolean shouldClear(MobEffect effect) {
        ResourceLocation effectId =
            ForgeRegistries.MOB_EFFECTS.getKey(effect);

        return effectId != null && effectIds.contains(effectId);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> definitions,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        Set<ResourceLocation> loadedEffectIds = new HashSet<>();

        definitions.forEach((definitionId, element) -> {
            try {
                loadedEffectIds.add(
                    parseEffectId(definitionId, element)
                );
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Skipping invalid Deathline Crossing clear effect "
                        + "definition {}: {}",
                    definitionId,
                    exception.getMessage()
                );
            }
        });

        effectIds = Set.copyOf(loadedEffectIds);
        LOGGER.info(
            "Loaded {} Deathline Crossing clear effect definitions",
            effectIds.size()
        );
    }

    private static ResourceLocation parseEffectId(
        ResourceLocation definitionId,
        JsonElement element
    ) {
        if (!element.isJsonObject()) {
            throw new JsonParseException(
                "definition must be a JSON object"
            );
        }

        JsonObject json = element.getAsJsonObject();
        String effectName = GsonHelper.getAsString(json, EFFECT_KEY);
        ResourceLocation effectId =
            ResourceLocation.tryParse(effectName);

        if (effectId == null) {
            throw new JsonParseException(
                "invalid effect id `" + effectName + "`"
            );
        }
        if (!ForgeRegistries.MOB_EFFECTS.containsKey(effectId)) {
            throw new JsonParseException(
                "unknown effect `" + effectId + "` in " + definitionId
            );
        }

        return effectId;
    }
}
