package com.magu1436.craftbound.occupations.explorer.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.magu1436.craftbound.occupations.explorer.discovery.StructureInstanceKey;

import net.minecraft.nbt.CompoundTag;

/** 発見履歴の可変なCapability実装。 */
public final class ExplorerDiscoveryDataImpl
    implements ExplorerDiscoveryData {

    private final Set<String> discoveredBiomes = new HashSet<>();
    private final Map<String, Set<StructureInstanceKey>>
        discoveredStructures = new HashMap<>();
    private final Set<String> discoveredDimensions = new HashSet<>();

    @Override
    public boolean hasDiscoveredBiome(String biomeId) {
        return discoveredBiomes.contains(requireId(biomeId));
    }

    @Override
    public boolean recordBiome(String biomeId) {
        return discoveredBiomes.add(requireId(biomeId));
    }

    @Override
    public boolean hasDiscoveredStructure(
        String structureId,
        StructureInstanceKey instanceKey
    ) {
        validateStructureKey(structureId, instanceKey);
        Set<StructureInstanceKey> instances = discoveredStructures.get(
            requireId(structureId)
        );
        return instances != null && instances.contains(instanceKey);
    }

    @Override
    public boolean recordStructure(
        String structureId,
        StructureInstanceKey instanceKey
    ) {
        validateStructureKey(structureId, instanceKey);
        return discoveredStructures.computeIfAbsent(
            requireId(structureId),
            ignored -> new HashSet<>()
        ).add(instanceKey);
    }

    @Override
    public int getStructureDiscoveryCount(String structureId) {
        Set<StructureInstanceKey> instances = discoveredStructures.get(
            requireId(structureId)
        );
        return instances == null ? 0 : instances.size();
    }

    @Override
    public boolean hasDiscoveredDimension(String dimensionId) {
        return discoveredDimensions.contains(requireId(dimensionId));
    }

    @Override
    public boolean recordDimension(String dimensionId) {
        return discoveredDimensions.add(requireId(dimensionId));
    }

    @Override
    public Set<String> discoveredBiomes() {
        return Set.copyOf(discoveredBiomes);
    }

    @Override
    public Map<String, Set<StructureInstanceKey>> discoveredStructures() {
        Map<String, Set<StructureInstanceKey>> copy = new HashMap<>();
        discoveredStructures.forEach(
            (id, instances) -> copy.put(id, Set.copyOf(instances))
        );
        return Map.copyOf(copy);
    }

    @Override
    public Set<String> discoveredDimensions() {
        return Set.copyOf(discoveredDimensions);
    }

    @Override
    public void replaceWith(
        Set<String> biomes,
        Map<String, Set<StructureInstanceKey>> structures,
        Set<String> dimensions
    ) {
        discoveredBiomes.clear();
        discoveredStructures.clear();
        discoveredDimensions.clear();

        Objects.requireNonNull(biomes, "biomes is null")
            .forEach(id -> discoveredBiomes.add(requireId(id)));
        Objects.requireNonNull(structures, "structures is null")
            .forEach((id, instances) -> discoveredStructures.put(
                requireId(id),
                new HashSet<>(Objects.requireNonNull(instances))
            ));
        Objects.requireNonNull(dimensions, "dimensions is null")
            .forEach(id -> discoveredDimensions.add(requireId(id)));
    }

    @Override
    public CompoundTag savePersistentData() {
        return ExplorerDiscoverySerializer.serialize(this);
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        ExplorerDiscoverySerializer.deserialize(tag, this);
    }

    @Override
    public void copyOnDeathFrom(ExplorerDiscoveryData original) {
        Objects.requireNonNull(original, "original is null");
        replaceWith(
            original.discoveredBiomes(),
            original.discoveredStructures(),
            original.discoveredDimensions()
        );
    }

    private static String requireId(String id) {
        Objects.requireNonNull(id, "resource id is null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("resource id is blank");
        }
        return id;
    }

    private static void validateStructureKey(
        String structureId,
        StructureInstanceKey instanceKey
    ) {
        requireId(structureId);
        Objects.requireNonNull(instanceKey, "instance key is null");
        if (!structureId.equals(instanceKey.structureId())) {
            throw new IllegalArgumentException(
                "structure id does not match instance key"
            );
        }
    }
}
