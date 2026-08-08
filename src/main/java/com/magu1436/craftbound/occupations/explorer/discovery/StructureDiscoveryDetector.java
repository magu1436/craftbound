package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoveryRegistry;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedStructureRule;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** 読み込み済みチャンクだけを使って、プレイヤーを含む構造物個体を検出する。 */
public final class StructureDiscoveryDetector {
    private final ExplorerDiscoveryRegistry registry;

    public StructureDiscoveryDetector(ExplorerDiscoveryRegistry registry) {
        this.registry = registry;
    }

    public Map<StructureInstanceKey, DiscoveryTarget.Structure> detect(
        ServerPlayer player
    ) {
        ServerLevel level = player.serverLevel();
        ChunkPos currentPos = player.chunkPosition();
        LevelChunk current = level.getChunkSource().getChunkNow(
            currentPos.x, currentPos.z
        );
        ExplorerDiscoveryData data = player.getCapability(
            CraftboundCapabilities.EXPLORER_DISCOVERY_DATA
        ).resolve().orElse(null);
        if (current == null || data == null) {
            return Map.of();
        }

        Map<StructureInstanceKey, DiscoveryTarget.Structure> found =
            new HashMap<>();
        Set<Structure> candidates = new HashSet<>(
            current.getAllReferences().keySet()
        );
        candidates.addAll(current.getAllStarts().keySet());
        candidates.forEach(structure -> {
            ResourceLocation id = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getKey(structure);
            if (id == null) {
                return;
            }
            ResolvedStructureRule rule = registry.findStructureRule(id)
                .filter(candidate -> candidate.enabled()
                    && candidate.maxDiscoveries() != 0)
                .orElse(null);
            if (rule == null || reachedLimit(data, id, rule)) {
                return;
            }

            Set<Long> starts = new HashSet<>();
            LongIterator iterator = current.getReferencesForStructure(
                structure
            ).iterator();
            while (iterator.hasNext()) {
                starts.add(iterator.nextLong());
            }
            if (current.getAllStarts().containsKey(structure)) {
                starts.add(currentPos.toLong());
            }
            for (long packed : starts) {
                inspectStart(level, player, structure, id, rule, data,
                    new ChunkPos(packed), found);
            }
        });
        return Map.copyOf(found);
    }

    private static void inspectStart(
        ServerLevel level,
        ServerPlayer player,
        Structure structure,
        ResourceLocation id,
        ResolvedStructureRule rule,
        ExplorerDiscoveryData data,
        ChunkPos startPos,
        Map<StructureInstanceKey, DiscoveryTarget.Structure> found
    ) {
        LevelChunk startChunk = level.getChunkSource().getChunkNow(
            startPos.x, startPos.z
        );
        if (startChunk == null) {
            return;
        }
        StructureStart start = startChunk.getStartForStructure(structure);
        if (start == null || !start.isValid() || !contains(start, player)) {
            return;
        }
        StructureInstanceKey key = new StructureInstanceKey(
            level.dimension().location().toString(), id.toString(),
            startPos.x, startPos.z
        );
        if (data.hasDiscoveredStructure(id.toString(), key)) {
            return;
        }
        found.put(key, new DiscoveryTarget.Structure(
            id, key, rule.xp(), rule.dwellTicks(), rule.maxDiscoveries(),
            rule.translationKey().orElse(null)
        ));
    }

    private static boolean contains(
        StructureStart start,
        ServerPlayer player
    ) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece.getBoundingBox().isInside(player.blockPosition())) {
                return true;
            }
        }
        return start.getBoundingBox().isInside(player.blockPosition());
    }

    private static boolean reachedLimit(
        ExplorerDiscoveryData data,
        ResourceLocation id,
        ResolvedStructureRule rule
    ) {
        return rule.maxDiscoveries() != -1
            && data.getStructureDiscoveryCount(id.toString())
                >= rule.maxDiscoveries();
    }
}
