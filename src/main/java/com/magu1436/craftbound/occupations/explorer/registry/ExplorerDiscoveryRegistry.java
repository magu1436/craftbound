package com.magu1436.craftbound.occupations.explorer.registry;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedBiomeRule;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedDimensionRule;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoverySnapshot.ResolvedStructureRule;

/** 完成済みSnapshotをアトミックに公開するRegistry。 */
public final class ExplorerDiscoveryRegistry {
    public static final ExplorerDiscoveryRegistry INSTANCE =
        new ExplorerDiscoveryRegistry();

    private volatile ExplorerDiscoverySnapshot currentSnapshot =
        ExplorerDiscoverySnapshot.empty();
    private volatile Runnable reloadCompletedCallback = () -> {
    };

    private ExplorerDiscoveryRegistry() {
    }

    public ExplorerDiscoverySnapshot current() {
        return currentSnapshot;
    }

    public Optional<ResolvedBiomeRule> findBiomeRule(
        ResourceLocation biomeId
    ) {
        return currentSnapshot.findBiome(biomeId);
    }

    public Optional<ResolvedStructureRule> findStructureRule(
        ResourceLocation structureId
    ) {
        return currentSnapshot.findStructure(structureId);
    }

    public Optional<ResolvedDimensionRule> findDimensionRule(
        ResourceLocation dimensionId
    ) {
        return currentSnapshot.findDimension(dimensionId);
    }

    public void publish(ExplorerDiscoverySnapshot snapshot) {
        currentSnapshot = Objects.requireNonNull(snapshot, "snapshot is null");
    }

    public void setReloadCompletedCallback(Runnable callback) {
        reloadCompletedCallback = Objects.requireNonNull(
            callback,
            "callback is null"
        );
    }

    void notifyReloadCompleted() {
        reloadCompletedCallback.run();
    }
}
