package com.magu1436.craftbound.occupations.adventurer.experience;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.magu1436.craftbound.Craftbound;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/**
 * データパックからMob経験値定義を読み込む。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MobExperienceReloadListener
    implements ResourceManagerReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY =
        "craftbound/adventurer/mob_experience";
    private static final int UNKNOWN_PACK_PRIORITY =
        Integer.MIN_VALUE;
    private static final MobExperienceReloadListener INSTANCE =
        new MobExperienceReloadListener();

    private MobExperienceReloadListener() {
    }

    @SubscribeEvent
    public static void addReloadListener(
        AddReloadListenerEvent event
    ) {
        event.addListener(INSTANCE);
    }

    @Override
    public void onResourceManagerReload(
        ResourceManager resourceManager
    ) {
        Map<String, Integer> packPriorities =
            collectPackPriorities(resourceManager);
        List<Candidate> candidates = loadCandidates(
            resourceManager,
            packPriorities
        );
        Map<EntityType<?>, MobExperienceDefinition> definitions =
            resolveCandidates(candidates);

        MobExperienceRegistry.replace(definitions);
        LOGGER.info(
            "Loaded {} adventurer mob experience definitions",
            definitions.size()
        );
    }

    private static Map<String, Integer> collectPackPriorities(
        ResourceManager resourceManager
    ) {
        List<PackResources> packs =
            resourceManager.listPacks().toList();
        Map<String, Integer> priorities = new HashMap<>();

        for (int index = 0; index < packs.size(); index++) {
            priorities.put(packs.get(index).packId(), index);
        }

        return priorities;
    }

    private static List<Candidate> loadCandidates(
        ResourceManager resourceManager,
        Map<String, Integer> packPriorities
    ) {
        Map<ResourceLocation, List<Resource>> resourceStacks =
            resourceManager.listResourceStacks(
                DIRECTORY,
                id -> id.getPath().endsWith(".json")
            );
        List<Candidate> candidates = new ArrayList<>();

        for (
            Map.Entry<
                ResourceLocation,
                List<Resource>
            > resourceEntry : resourceStacks.entrySet()
        ) {
            for (Resource resource : resourceEntry.getValue()) {
                loadCandidate(
                    resourceEntry.getKey(),
                    resource,
                    packPriorities
                ).ifPresent(candidates::add);
            }
        }

        return candidates;
    }

    private static Optional<Candidate> loadCandidate(
        ResourceLocation resourceId,
        Resource resource,
        Map<String, Integer> packPriorities
    ) {
        String packId = resource.sourcePackId();
        Integer priority = packPriorities.get(packId);

        if (priority == null) {
            priority = UNKNOWN_PACK_PRIORITY;
            LOGGER.warn(
                "Could not determine data pack priority for {} from {}",
                resourceId,
                packId
            );
        }

        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            MobExperienceDefinition definition =
                MobExperienceDefinitionParser.parse(root);

            return Optional.of(
                new Candidate(
                    resourceId,
                    packId,
                    priority,
                    definition
                )
            );
        } catch (
            IOException
                | JsonParseException
                | IllegalArgumentException exception
        ) {
            LOGGER.warn(
                "Skipping invalid mob experience definition {} from {}: {}",
                resourceId,
                packId,
                exception.getMessage()
            );
            return Optional.empty();
        }
    }

    private static Map<
        EntityType<?>,
        MobExperienceDefinition
    > resolveCandidates(List<Candidate> candidates) {
        Map<EntityType<?>, List<Candidate>> candidatesByEntity =
            new LinkedHashMap<>();

        for (Candidate candidate : candidates) {
            candidatesByEntity
                .computeIfAbsent(
                    candidate.definition().entityType(),
                    ignored -> new ArrayList<>()
                )
                .add(candidate);
        }

        Map<EntityType<?>, MobExperienceDefinition> resolved =
            new LinkedHashMap<>();

        for (
            Map.Entry<
                EntityType<?>,
                List<Candidate>
            > entityEntry : candidatesByEntity.entrySet()
        ) {
            List<Candidate> entityCandidates =
                entityEntry.getValue();
            int highestPriority = entityCandidates.stream()
                .mapToInt(Candidate::packPriority)
                .max()
                .orElse(UNKNOWN_PACK_PRIORITY);
            List<Candidate> highestPriorityCandidates =
                entityCandidates.stream()
                    .filter(candidate ->
                        candidate.packPriority() == highestPriority
                    )
                    .toList();

            if (highestPriorityCandidates.size() > 1) {
                warnDuplicateDefinitions(
                    highestPriorityCandidates
                );
                continue;
            }

            Candidate selected =
                highestPriorityCandidates.get(0);
            resolved.put(
                entityEntry.getKey(),
                selected.definition()
            );
        }

        return resolved;
    }

    private static void warnDuplicateDefinitions(
        List<Candidate> candidates
    ) {
        Candidate first = candidates.get(0);
        String resources = candidates.stream()
            .map(candidate ->
                candidate.resourceId().toString()
            )
            .collect(Collectors.joining(", "));

        LOGGER.warn(
            "Ignoring duplicate mob experience definitions for {} "
                + "in data pack {} at priority {}: {}",
            first.definition().entityId(),
            first.packId(),
            first.packPriority(),
            resources
        );
    }

    private record Candidate(
        ResourceLocation resourceId,
        String packId,
        int packPriority,
        MobExperienceDefinition definition
    ) {
    }
}
