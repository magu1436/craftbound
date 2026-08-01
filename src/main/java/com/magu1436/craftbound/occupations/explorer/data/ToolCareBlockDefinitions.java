package com.magu1436.craftbound.occupations.explorer.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

/**
 * 道具の手入れが発動するブロックのデータ定義。
 */
public final class ToolCareBlockDefinitions
    extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY =
        "craftbound/explorer/tool_care_blocks";
    private static final String BLOCKS_KEY = "blocks";

    public static final ToolCareBlockDefinitions INSTANCE =
        new ToolCareBlockDefinitions();

    private volatile Set<ResourceLocation> blockIds = Set.of();

    private ToolCareBlockDefinitions() {
        super(GSON, DIRECTORY);
    }

    /**
     * 指定したブロックが道具の手入れの対象かを返す。
     *
     * @param block ブロック
     * @return 対象の場合は {@code true}
     */
    public boolean contains(Block block) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        return blockId != null && blockIds.contains(blockId);
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> definitions,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        Set<ResourceLocation> loadedBlockIds = new HashSet<>();

        definitions.forEach((definitionId, element) -> {
            try {
                loadedBlockIds.addAll(
                    parseBlockIds(definitionId, element)
                );
            } catch (RuntimeException exception) {
                LOGGER.warn(
                    "Skipping invalid Tool Care block definition {}: {}",
                    definitionId,
                    exception.getMessage()
                );
            }
        });

        blockIds = Set.copyOf(loadedBlockIds);
        LOGGER.info(
            "Loaded {} Tool Care blocks",
            blockIds.size()
        );
    }

    private static Set<ResourceLocation> parseBlockIds(
        ResourceLocation definitionId,
        JsonElement element
    ) {
        if (!element.isJsonObject()) {
            throw new JsonParseException(
                "definition must be a JSON object"
            );
        }

        JsonObject json = element.getAsJsonObject();
        JsonArray blocks = GsonHelper.getAsJsonArray(json, BLOCKS_KEY);
        Set<ResourceLocation> parsedBlockIds = new HashSet<>();

        for (JsonElement blockElement : blocks) {
            if (!blockElement.isJsonPrimitive()
                || !blockElement.getAsJsonPrimitive().isString()) {
                throw new JsonParseException(
                    "every `blocks` entry must be a string"
                );
            }

            String blockName = blockElement.getAsString();
            ResourceLocation blockId =
                ResourceLocation.tryParse(blockName);

            if (blockId == null) {
                throw new JsonParseException(
                    "invalid block id `" + blockName + "`"
                );
            }
            if (!ForgeRegistries.BLOCKS.containsKey(blockId)) {
                throw new JsonParseException(
                    "unknown block `" + blockId + "` in "
                        + definitionId
                );
            }

            parsedBlockIds.add(blockId);
        }

        return parsedBlockIds;
    }
}
