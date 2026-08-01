package com.magu1436.craftbound.occupations.architect.experience;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.magu1436.craftbound.occupations.architect.ArchitectConfig;
import com.magu1436.craftbound.occupations.architect.capability.ArchitectDataAccess;

/**
 * 成立した施工の経験値をオンラインまたは保留データへ渡す。
 */
public final class ArchitectExperienceService {
    private ArchitectExperienceService() {
    }

    public static void grant(
        ServerLevel level,
        BlockPos pos,
        UUID playerId,
        BlockState state,
        int itemUseCountSnapshot,
        int recentUseCountSnapshot
    ) {
        long pointUnits = ConstructionPointCalculator.calculate(
            level,
            pos,
            state,
            itemUseCountSnapshot,
            recentUseCountSnapshot
        );
        if (pointUnits <= 0L) {
            return;
        }
        ServerPlayer player = level.getServer()
            .getPlayerList()
            .getPlayer(playerId);
        if (player != null && awardPointUnits(player, pointUnits)) {
            return;
        }
        ArchitectPendingExperienceData
            .get(level.getServer())
            .add(playerId, pointUnits);
    }

    public static void deliverPending(ServerPlayer player) {
        ArchitectPendingExperienceData data =
            ArchitectPendingExperienceData.get(player.getServer());
        long pointUnits = data.take(player.getUUID());
        if (pointUnits > 0L && !awardPointUnits(player, pointUnits)) {
            data.add(player.getUUID(), pointUnits);
        }
    }

    private static boolean awardPointUnits(
        ServerPlayer player,
        long pointUnits
    ) {
        return ArchitectDataAccess.get(player).map(data -> {
            long experience = data.addPointUnits(
                pointUnits,
                ArchitectConfig.pointUnitsPerExperience()
            );
            while (experience > 0L) {
                int awarded = (int) Math.min(
                    experience,
                    Integer.MAX_VALUE
                );
                ArchitectConstructionExperienceSource.award(player, awarded);
                experience -= awarded;
            }
            return true;
        }).orElse(false);
    }
}
