package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.magu1436.craftbound.occupations.architect.ConstructionTrackingService;
import com.magu1436.craftbound.occupations.architect.capability.ConstructionChunkDataAccess;
import com.magu1436.craftbound.occupations.architect.capability.PendingConstruction;

/**
 * 施工候補の残存を確認し、一度だけ経験値付与を確定する。
 */
public final class ConstructionConfirmationService {

    private ConstructionConfirmationService() {
    }

    public static void confirm(
        ServerLevel level,
        BlockPos pos,
        long scheduledMatureAt
    ) {
        PendingConstruction pending = ConstructionChunkDataAccess
            .getPending(level, pos)
            .orElse(null);
        if (pending == null
            || pending.matureAtGameTime() != scheduledMatureAt) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        boolean remains = ConstructionChunkDataAccess.isPlayerPlaced(
            level,
            pos
        ) && !state.isAir()
            && BuiltInRegistries.BLOCK
                .getKey(state.getBlock())
                .equals(pending.expectedBlockId());
        if (!remains) {
            ConstructionTrackingService.onRemoved(level, pos);
            return;
        }

        if (!ConstructionChunkDataAccess.tryMarkXpRewarded(level, pos)) {
            ConstructionChunkDataAccess.removePending(level, pos);
            return;
        }
        ConstructionChunkDataAccess.removePending(level, pos);
        ArchitectExperienceService.grant(
            level,
            pending.playerId(),
            state,
            pending.lifetimeUseCountSnapshot()
        );
    }
}
