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

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import org.slf4j.Logger;

/**
 * 死線踏破の発動と保護から除外するダメージのデータ定義。
 */
public final class DeathlineExcludedDamageDefinitions
    extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY =
        "craftbound/adventurer/deathline_excluded_damage";
    private static final String DAMAGE_TYPE_KEY = "damage_type";
    private static final String DAMAGE_TAG_KEY = "damage_tag";

    public static final DeathlineExcludedDamageDefinitions INSTANCE =
        new DeathlineExcludedDamageDefinitions();

    private volatile DefinitionSnapshot snapshot =
        DefinitionSnapshot.EMPTY;

    private DeathlineExcludedDamageDefinitions() {
        super(GSON, DIRECTORY);
    }

    /**
     * 指定したダメージが除外対象かを返す。
     *
     * @param source ダメージ
     * @return 除外対象の場合は {@code true}
     */
    public boolean matches(DamageSource source) {
        DefinitionSnapshot currentSnapshot = snapshot;

        return currentSnapshot
            .damageTypes()
            .stream()
            .anyMatch(source::is)
            || currentSnapshot
                .damageTags()
                .stream()
                .anyMatch(source::is);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> definitions,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        Set<ResourceKey<DamageType>> loadedDamageTypes =
            new HashSet<>();
        Set<TagKey<DamageType>> loadedDamageTags = new HashSet<>();

        definitions.forEach((definitionId, element) -> {
            try {
                ParsedDefinition definition = parseDefinition(
                    element
                );

                if (definition.damageType() != null) {
                    loadedDamageTypes.add(definition.damageType());
                } else {
                    loadedDamageTags.add(definition.damageTag());
                }
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Skipping invalid Deathline Crossing excluded damage "
                        + "definition {}: {}",
                    definitionId,
                    exception.getMessage()
                );
            }
        });

        snapshot = new DefinitionSnapshot(
            Set.copyOf(loadedDamageTypes),
            Set.copyOf(loadedDamageTags)
        );
        LOGGER.info(
            "Loaded {} Deathline Crossing excluded damage definitions",
            loadedDamageTypes.size() + loadedDamageTags.size()
        );
    }

    private static ParsedDefinition parseDefinition(
        JsonElement element
    ) {
        if (!element.isJsonObject()) {
            throw new JsonParseException(
                "definition must be a JSON object"
            );
        }

        JsonObject json = element.getAsJsonObject();
        boolean hasDamageType = json.has(DAMAGE_TYPE_KEY);
        boolean hasDamageTag = json.has(DAMAGE_TAG_KEY);

        if (hasDamageType == hasDamageTag) {
            throw new JsonParseException(
                "exactly one of `damage_type` or `damage_tag` is required"
            );
        }

        if (hasDamageType) {
            ResourceLocation id = parseResourceLocation(
                GsonHelper.getAsString(json, DAMAGE_TYPE_KEY),
                DAMAGE_TYPE_KEY
            );

            return new ParsedDefinition(
                ResourceKey.create(Registries.DAMAGE_TYPE, id),
                null
            );
        }

        ResourceLocation id = parseResourceLocation(
            GsonHelper.getAsString(json, DAMAGE_TAG_KEY),
            DAMAGE_TAG_KEY
        );

        return new ParsedDefinition(
            null,
            TagKey.create(Registries.DAMAGE_TYPE, id)
        );
    }

    private static ResourceLocation parseResourceLocation(
        String value,
        String fieldName
    ) {
        ResourceLocation id = ResourceLocation.tryParse(value);

        if (id == null) {
            throw new JsonParseException(
                "invalid `" + fieldName + "` id `" + value + "`"
            );
        }

        return id;
    }

    private record ParsedDefinition(
        ResourceKey<DamageType> damageType,
        TagKey<DamageType> damageTag
    ) {
    }

    private record DefinitionSnapshot(
        Set<ResourceKey<DamageType>> damageTypes,
        Set<TagKey<DamageType>> damageTags
    ) {
        private static final DefinitionSnapshot EMPTY =
            new DefinitionSnapshot(Set.of(), Set.of());
    }
}
