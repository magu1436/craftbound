package com.magu1436.craftbound.occupations.explorer.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.magu1436.craftbound.occupations.explorer.discovery.StructureInstanceKey;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Version 1の発見履歴NBT形式を読み書きする。 */
public final class ExplorerDiscoverySerializer {
    public static final int CURRENT_VERSION = 1;

    private static final String VERSION = "Version";
    private static final String BIOMES = "Biomes";
    private static final String STRUCTURES = "Structures";
    private static final String DIMENSIONS = "Dimensions";
    private static final String DIMENSION = "Dimension";
    private static final String STRUCTURE = "Structure";
    private static final String START_CHUNK_X = "StartChunkX";
    private static final String START_CHUNK_Z = "StartChunkZ";

    private ExplorerDiscoverySerializer() {
    }

    public static CompoundTag serialize(ExplorerDiscoveryData data) {
        Objects.requireNonNull(data, "data is null");
        CompoundTag root = new CompoundTag();
        root.putInt(VERSION, CURRENT_VERSION);
        root.put(BIOMES, stringList(data.discoveredBiomes()));
        root.put(DIMENSIONS, stringList(data.discoveredDimensions()));

        ListTag structures = new ListTag();
        data.discoveredStructures().forEach((structureId, instances) -> {
            for (StructureInstanceKey key : instances) {
                CompoundTag entry = new CompoundTag();
                entry.putString(DIMENSION, key.dimensionId());
                entry.putString(STRUCTURE, structureId);
                entry.putInt(START_CHUNK_X, key.startChunkX());
                entry.putInt(START_CHUNK_Z, key.startChunkZ());
                structures.add(entry);
            }
        });
        root.put(STRUCTURES, structures);
        return root;
    }

    public static void deserialize(
        CompoundTag root,
        ExplorerDiscoveryData destination
    ) {
        Objects.requireNonNull(root, "root is null");
        Objects.requireNonNull(destination, "destination is null");

        if (
            !root.contains(VERSION, Tag.TAG_INT)
                || root.getInt(VERSION) != CURRENT_VERSION
        ) {
            destination.replaceWith(Set.of(), Map.of(), Set.of());
            return;
        }

        Set<String> biomes = readStringSet(root, BIOMES);
        Set<String> dimensions = readStringSet(root, DIMENSIONS);
        Map<String, Set<StructureInstanceKey>> structures = new HashMap<>();
        ListTag entries = root.getList(STRUCTURES, Tag.TAG_COMPOUND);

        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (
                !entry.contains(DIMENSION, Tag.TAG_STRING)
                    || !entry.contains(STRUCTURE, Tag.TAG_STRING)
                    || !entry.contains(START_CHUNK_X, Tag.TAG_INT)
                    || !entry.contains(START_CHUNK_Z, Tag.TAG_INT)
            ) {
                continue;
            }
            String dimensionId = entry.getString(DIMENSION);
            String structureId = entry.getString(STRUCTURE);
            if (dimensionId.isBlank() || structureId.isBlank()) {
                continue;
            }
            StructureInstanceKey key = new StructureInstanceKey(
                dimensionId,
                structureId,
                entry.getInt(START_CHUNK_X),
                entry.getInt(START_CHUNK_Z)
            );
            structures.computeIfAbsent(
                structureId,
                ignored -> new HashSet<>()
            ).add(key);
        }
        destination.replaceWith(biomes, structures, dimensions);
    }

    private static ListTag stringList(Set<String> values) {
        ListTag result = new ListTag();
        values.stream().sorted().map(StringTag::valueOf).forEach(result::add);
        return result;
    }

    private static Set<String> readStringSet(CompoundTag root, String key) {
        Set<String> values = new HashSet<>();
        ListTag list = root.getList(key, Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            String value = list.getString(index);
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }
}
