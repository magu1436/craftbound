package com.magu1436.craftbound.occupations.explorer.registry;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedBiomeRule;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedDimensionRule;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedStructureRule;
import com.magu1436.craftbound.occupations.explorer.registry.StructureDiscoveryRule.StructureSelector;
import com.mojang.logging.LogUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/** 探検家発見定義を解析し、解決済みSnapshotへ一括交換する。 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ExplorerDiscoveryReloadListener
    extends SimplePreparableReloadListener<ExplorerDiscoveryDefinitions> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "craftbound/explorer/";
    private static final String TIER_DIRECTORY = ROOT + "experience_tiers";
    private static final String BIOME_DIRECTORY = ROOT + "biomes";
    private static final String STRUCTURE_DIRECTORY = ROOT + "structures";
    private static final String DIMENSION_DIRECTORY = ROOT + "dimensions";

    private final ExplorerDiscoveryRegistry registry;
    private final RegistryAccess registryAccess;

    private ExplorerDiscoveryReloadListener(
        ExplorerDiscoveryRegistry registry,
        RegistryAccess registryAccess
    ) {
        this.registry = registry;
        this.registryAccess = registryAccess;
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ExplorerDiscoveryReloadListener(
            ExplorerDiscoveryRegistry.INSTANCE,
            event.getRegistryAccess()
        ));
    }

    @Override
    protected ExplorerDiscoveryDefinitions prepare(
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        ExplorerDefinitionErrors errors = new ExplorerDefinitionErrors();
        return new ExplorerDiscoveryDefinitions(
            loadTiers(resourceManager, errors),
            loadRules(
                resourceManager,
                BIOME_DIRECTORY,
                errors,
                BiomeDiscoveryRule::parse
            ),
            loadRules(
                resourceManager,
                STRUCTURE_DIRECTORY,
                errors,
                StructureDiscoveryRule::parse
            ),
            loadRules(
                resourceManager,
                DIMENSION_DIRECTORY,
                errors,
                DimensionDiscoveryRule::parse
            ),
            errors
        );
    }

    @Override
    protected void apply(
        ExplorerDiscoveryDefinitions definitions,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        ExplorerDiscoverySnapshot snapshot = resolve(definitions);
        registry.publish(snapshot);
        registry.notifyReloadCompleted();
        definitions.errors().logAll(LOGGER);
        LOGGER.info(
            "Loaded explorer discovery definitions: {} tiers, {} biomes, "
                + "{} structures, {} dimensions",
            snapshot.experienceTiers().size(),
            snapshot.biomeRules().size(),
            snapshot.structureRules().size(),
            snapshot.dimensionRules().size()
        );
    }

    private ExplorerDiscoverySnapshot resolve(
        ExplorerDiscoveryDefinitions definitions
    ) {
        Map<ResourceLocation, ExperienceTier> tiers =
            new HashMap<>(definitions.experienceTiers());
        if (tiers.remove(ExperienceSpec.SPECIAL) != null) {
            definitions.errors().add(
                ExperienceSpec.SPECIAL,
                "craftbound:special is reserved and cannot be a tier file"
            );
        }
        return new ExplorerDiscoverySnapshot(
            tiers,
            resolveBiomes(definitions.biomeRules(), tiers, definitions.errors()),
            resolveStructures(
                definitions.structureRules(), tiers, definitions.errors()
            ),
            resolveDimensions(
                definitions.dimensionRules(), tiers, definitions.errors()
            )
        );
    }

    private Map<ResourceLocation, ResolvedBiomeRule> resolveBiomes(
        List<BiomeDiscoveryRule> rules,
        Map<ResourceLocation, ExperienceTier> tiers,
        ExplorerDefinitionErrors errors
    ) {
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(
            Registries.BIOME
        );
        Map<ResourceLocation, BiomeDiscoveryRule> unique = new HashMap<>();
        Set<ResourceLocation> conflicts = new HashSet<>();
        for (BiomeDiscoveryRule rule : rules) {
            if (!biomeRegistry.containsKey(rule.biomeId())) {
                errors.add(
                    rule.definitionId(),
                    "Unknown biome: " + rule.biomeId()
                );
                continue;
            }
            BiomeDiscoveryRule previous = unique.putIfAbsent(
                rule.biomeId(), rule
            );
            if (previous != null) {
                conflicts.add(rule.biomeId());
                addConflict(errors, rule.definitionId(), previous.definitionId(),
                    "biome", rule.biomeId());
            }
        }

        Map<ResourceLocation, ResolvedBiomeRule> resolved = new HashMap<>();
        unique.forEach((id, rule) -> {
            if (conflicts.contains(id)) {
                return;
            }
            if (!rule.enabled()) {
                resolved.put(id, ResolvedBiomeRule.disabled(id));
                return;
            }
            OptionalInt xp = rule.experience().resolveXp(tiers);
            if (xp.isEmpty()) {
                errors.add(
                    rule.definitionId(),
                    "Unknown experience tier: " + rule.experience().tierId()
                );
                return;
            }
            resolved.put(id, new ResolvedBiomeRule(
                id, true, xp.getAsInt(), rule.dwellTicks(),
                rule.translationKey()
            ));
        });
        return resolved;
    }

    private Map<ResourceLocation, ResolvedStructureRule> resolveStructures(
        List<StructureDiscoveryRule> rules,
        Map<ResourceLocation, ExperienceTier> tiers,
        ExplorerDefinitionErrors errors
    ) {
        Registry<Structure> structures = registryAccess.registryOrThrow(
            Registries.STRUCTURE
        );
        Map<ResourceLocation, StructureDiscoveryRule> direct = new HashMap<>();
        Set<ResourceLocation> directConflicts = new HashSet<>();
        List<StructureDiscoveryRule> tagRules = new ArrayList<>();

        for (StructureDiscoveryRule rule : rules) {
            if (rule.selector() instanceof StructureSelector.Tag) {
                tagRules.add(rule);
                continue;
            }
            ResourceLocation id = ((StructureSelector.Direct) rule.selector())
                .structureId();
            if (!structures.containsKey(id)) {
                errors.add(rule.definitionId(), "Unknown structure: " + id);
                continue;
            }
            StructureDiscoveryRule previous = direct.putIfAbsent(id, rule);
            if (previous != null) {
                directConflicts.add(id);
                addConflict(errors, rule.definitionId(), previous.definitionId(),
                    "structure", id);
            }
        }

        Map<ResourceLocation, List<StructureDiscoveryRule>> expanded =
            new HashMap<>();
        for (StructureDiscoveryRule rule : tagRules) {
            ResourceLocation tagId = ((StructureSelector.Tag) rule.selector())
                .tagId();
            TagKey<Structure> tagKey = TagKey.create(
                Registries.STRUCTURE, tagId
            );
            HolderSet.Named<Structure> values = structures.getTag(tagKey)
                .orElse(null);
            if (values == null || values.size() == 0) {
                errors.add(
                    rule.definitionId(),
                    "Unknown or empty structure tag: " + tagId
                );
                continue;
            }
            for (Holder<Structure> holder : values) {
                holder.unwrapKey().ifPresent(key -> expanded.computeIfAbsent(
                    key.location(), ignored -> new ArrayList<>()
                ).add(rule));
            }
        }

        Map<ResourceLocation, ResolvedStructureRule> resolved = new HashMap<>();
        for (ResourceLocation id : structures.keySet()) {
            if (directConflicts.contains(id)) {
                continue;
            }
            StructureDiscoveryRule selected = direct.get(id);
            if (selected == null) {
                List<StructureDiscoveryRule> matches = expanded.getOrDefault(
                    id, List.of()
                );
                if (matches.size() > 1) {
                    matches.forEach(rule -> errors.add(
                        rule.definitionId(),
                        "Multiple structure tags match target " + id
                    ));
                    continue;
                }
                if (matches.isEmpty()) {
                    continue;
                }
                selected = matches.get(0);
            }
            ResolvedStructureRule rule = resolveStructureRule(
                id, selected, tiers, errors
            );
            if (rule != null) {
                resolved.put(id, rule);
            }
        }
        return resolved;
    }

    private static ResolvedStructureRule resolveStructureRule(
        ResourceLocation id,
        StructureDiscoveryRule rule,
        Map<ResourceLocation, ExperienceTier> tiers,
        ExplorerDefinitionErrors errors
    ) {
        if (!rule.enabled()) {
            return ResolvedStructureRule.disabled(id);
        }
        OptionalInt xp = rule.experience().resolveXp(tiers);
        if (xp.isEmpty()) {
            errors.add(
                rule.definitionId(),
                "Unknown experience tier: " + rule.experience().tierId()
            );
            return null;
        }
        return new ResolvedStructureRule(
            id,
            true,
            xp.getAsInt(),
            rule.dwellTicks(),
            rule.maxDiscoveries(),
            rule.translationKey()
        );
    }

    private Map<ResourceLocation, ResolvedDimensionRule> resolveDimensions(
        List<DimensionDiscoveryRule> rules,
        Map<ResourceLocation, ExperienceTier> tiers,
        ExplorerDefinitionErrors errors
    ) {
        Set<ResourceLocation> dimensionIds = registryAccess.registryOrThrow(
            Registries.LEVEL_STEM
        ).keySet();
        Map<ResourceLocation, DimensionDiscoveryRule> unique = new HashMap<>();
        Set<ResourceLocation> conflicts = new HashSet<>();
        for (DimensionDiscoveryRule rule : rules) {
            if (!dimensionIds.contains(rule.dimensionId())) {
                errors.add(
                    rule.definitionId(),
                    "Unknown dimension: " + rule.dimensionId()
                );
                continue;
            }
            DimensionDiscoveryRule previous = unique.putIfAbsent(
                rule.dimensionId(), rule
            );
            if (previous != null) {
                conflicts.add(rule.dimensionId());
                addConflict(errors, rule.definitionId(), previous.definitionId(),
                    "dimension", rule.dimensionId());
            }
        }

        Map<ResourceLocation, ResolvedDimensionRule> resolved = new HashMap<>();
        unique.forEach((id, rule) -> {
            if (conflicts.contains(id)) {
                return;
            }
            if (!rule.enabled()) {
                resolved.put(id, ResolvedDimensionRule.disabled(id));
                return;
            }
            OptionalInt xp = rule.experience().resolveXp(tiers);
            if (xp.isEmpty()) {
                errors.add(
                    rule.definitionId(),
                    "Unknown experience tier: " + rule.experience().tierId()
                );
                return;
            }
            resolved.put(id, new ResolvedDimensionRule(
                id, true, xp.getAsInt(), rule.dwellTicks(),
                rule.translationKey()
            ));
        });
        return resolved;
    }

    private static Map<ResourceLocation, ExperienceTier> loadTiers(
        ResourceManager resources,
        ExplorerDefinitionErrors errors
    ) {
        Map<ResourceLocation, ExperienceTier> result = new HashMap<>();
        resources.listResources(
            TIER_DIRECTORY,
            id -> id.getPath().endsWith(".json")
        ).forEach((fileId, resource) -> {
            ResourceLocation definitionId = definitionId(
                fileId, TIER_DIRECTORY
            );
            JsonObject json = readJson(resource, fileId, errors);
            if (definitionId == null || json == null) {
                return;
            }
            ExplorerParseResult<ExperienceTier> parsed = ExperienceTier.parse(
                definitionId, json, errors
            );
            if (parsed.isSuccess()) {
                result.put(definitionId, parsed.value());
            }
        });
        return result;
    }

    private static <T> List<T> loadRules(
        ResourceManager resources,
        String directory,
        ExplorerDefinitionErrors errors,
        RuleParser<T> parser
    ) {
        List<T> result = new ArrayList<>();
        resources.listResources(
            directory,
            id -> id.getPath().endsWith(".json")
        ).forEach((fileId, resource) -> {
            JsonObject json = readJson(resource, fileId, errors);
            if (json == null) {
                return;
            }
            ExplorerParseResult<T> parsed = parser.parse(
                fileId, json, errors
            );
            if (parsed.isSuccess()) {
                result.add(parsed.value());
            }
        });
        return result;
    }

    private static JsonObject readJson(
        Resource resource,
        ResourceLocation fileId,
        ExplorerDefinitionErrors errors
    ) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                errors.add(fileId, "Definition root must be an object");
                return null;
            }
            return root.getAsJsonObject();
        } catch (IOException | JsonParseException exception) {
            errors.add(fileId, "Could not parse JSON: " + exception.getMessage());
            return null;
        }
    }

    private static ResourceLocation definitionId(
        ResourceLocation fileId,
        String directory
    ) {
        String prefix = directory + "/";
        String path = fileId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(
            fileId.getNamespace(),
            path.substring(prefix.length(), path.length() - 5)
        );
    }

    private static void addConflict(
        ExplorerDefinitionErrors errors,
        ResourceLocation current,
        ResourceLocation previous,
        String type,
        ResourceLocation target
    ) {
        errors.add(
            current,
            "Duplicate " + type + " target " + target
                + "; also defined by " + previous
        );
    }

    @FunctionalInterface
    private interface RuleParser<T> {
        ExplorerParseResult<T> parse(
            ResourceLocation definitionId,
            JsonObject json,
            ExplorerDefinitionErrors errors
        );
    }
}
