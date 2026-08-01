package com.magu1436.craftbound.occupations.architect.experience;

import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 成立した施工の経験値をオンラインまたは保留データへ渡す。
 */
public final class ArchitectExperienceService {
    private static final int BASE_CONSTRUCTION_EXPERIENCE = 1;

    private ArchitectExperienceService() {
    }

    public static void grant(
        ServerLevel level,
        UUID playerId,
        BlockState state,
        int itemUseCountSnapshot
    ) {
        int points = calculatePoints(state, itemUseCountSnapshot);
        ServerPlayer player = level.getServer()
            .getPlayerList()
            .getPlayer(playerId);
        if (player != null) {
            ArchitectConstructionExperienceSource.award(player, points);
            return;
        }
        ArchitectPendingExperienceData
            .get(level.getServer())
            .add(playerId, points);
    }

    public static void deliverPending(ServerPlayer player) {
        ArchitectPendingExperienceData data =
            ArchitectPendingExperienceData.get(player.getServer());
        long points = data.take(player.getUUID());
        while (points > 0L) {
            int awarded = (int) Math.min(points, Integer.MAX_VALUE);
            ArchitectConstructionExperienceSource.award(player, awarded);
            points -= awarded;
        }
    }

    private static int calculatePoints(
        BlockState state,
        int itemUseCountSnapshot
    ) {
        return BASE_CONSTRUCTION_EXPERIENCE;
    }
}
