package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/** ログアウトを越えて保持しない、プレイヤーごとの滞在候補。 */
public final class ExplorerCandidateTracker {
    private final Map<UUID, PlayerCandidateState> playerStates =
        new HashMap<>();

    public List<DiscoveryTarget> update(
        ServerPlayer player,
        ExplorerDiscoveryScanner.DiscoveryScanResult scan,
        long gameTime
    ) {
        Objects.requireNonNull(scan, "scan is null");
        return update(
            player,
            scan.biome(),
            scan.dimension(),
            scan.structures(),
            gameTime
        );
    }

    public List<DiscoveryTarget> update(
        ServerPlayer player,
        Optional<DiscoveryTarget.Biome> biome,
        Optional<DiscoveryTarget.Dimension> dimension,
        Map<StructureInstanceKey, DiscoveryTarget.Structure> structures,
        long gameTime
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(biome, "biome is null");
        Objects.requireNonNull(dimension, "dimension is null");
        Objects.requireNonNull(structures, "structures is null");

        PlayerCandidateState state = playerStates.computeIfAbsent(
            player.getUUID(),
            ignored -> new PlayerCandidateState()
        );
        List<DiscoveryTarget> ready = new ArrayList<>();

        state.biome = updateSingle(
            state.biome,
            biome.orElse(null),
            gameTime,
            ready
        );
        state.dimension = updateSingle(
            state.dimension,
            dimension.orElse(null),
            gameTime,
            ready
        );
        updateStructures(state, structures, gameTime, ready);

        return List.copyOf(ready);
    }

    public void markCompleted(UUID playerId, DiscoveryTarget target) {
        Objects.requireNonNull(playerId, "player id is null");
        Objects.requireNonNull(target, "target is null");
        PlayerCandidateState state = playerStates.get(playerId);
        if (state == null) {
            return;
        }

        switch (target.type()) {
            case BIOME -> state.biome = null;
            case DIMENSION -> state.dimension = null;
            case STRUCTURE -> state.structures.remove(
                ((DiscoveryTarget.Structure) target).instanceKey()
            );
        }
        removeEmptyState(playerId, state);
    }

    public void clear(UUID playerId) {
        playerStates.remove(Objects.requireNonNull(playerId));
    }

    public void clearAll() {
        playerStates.clear();
    }

    private static TimedCandidate updateSingle(
        TimedCandidate previous,
        DiscoveryTarget current,
        long gameTime,
        List<DiscoveryTarget> ready
    ) {
        if (current == null) {
            return null;
        }
        if (
            previous == null
                || !previous.target().candidateKey().equals(
                    current.candidateKey()
                )
        ) {
            TimedCandidate candidate = new TimedCandidate(current, gameTime);
            if (current.dwellTicks() == 0) {
                ready.add(current);
            }
            return candidate;
        }
        if (gameTime - previous.enteredGameTime() >= current.dwellTicks()) {
            ready.add(current);
        }
        return previous;
    }

    private static void updateStructures(
        PlayerCandidateState state,
        Map<StructureInstanceKey, DiscoveryTarget.Structure> current,
        long gameTime,
        List<DiscoveryTarget> ready
    ) {
        Set<StructureInstanceKey> currentKeys = new HashSet<>(current.keySet());
        state.structures.keySet().removeIf(key -> !currentKeys.contains(key));

        current.forEach((key, target) -> {
            TimedCandidate previous = state.structures.get(key);
            if (
                previous == null
                    || !previous.target().candidateKey().equals(
                        target.candidateKey()
                    )
            ) {
                state.structures.put(
                    key,
                    new TimedCandidate(target, gameTime)
                );
                if (target.dwellTicks() == 0) {
                    ready.add(target);
                }
            } else if (
                gameTime - previous.enteredGameTime()
                    >= target.dwellTicks()
            ) {
                ready.add(target);
            }
        });
    }

    private void removeEmptyState(UUID playerId, PlayerCandidateState state) {
        if (
            state.biome == null
                && state.dimension == null
                && state.structures.isEmpty()
        ) {
            playerStates.remove(playerId);
        }
    }

    private record TimedCandidate(
        DiscoveryTarget target,
        long enteredGameTime
    ) {
    }

    private static final class PlayerCandidateState {
        private TimedCandidate biome;
        private TimedCandidate dimension;
        private final Map<StructureInstanceKey, TimedCandidate> structures =
            new HashMap<>();
    }
}
