package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

import com.magu1436.craftbound.occupations.architect.capability.PendingConstruction;
import com.magu1436.craftbound.occupations.architect.capability.ArchitectDataAccess;
import com.magu1436.craftbound.occupations.architect.ArchitectConfig;

/**
 * 設置時点の情報から施工候補を生成する。
 */
public final class PendingConstructionFactory {
    public static final long CONSTRUCTION_DWELL_TICKS = 2_400L;

    private PendingConstructionFactory() {
    }

    public static PendingConstruction create(
        ServerPlayer player,
        ServerLevel level,
        BlockState state
    ) {
        Item item = state.getBlock().asItem();
        int itemUseCount = player.getStats().getValue(
            Stats.ITEM_USED.get(item)
        );
        int recentUseCount = ArchitectDataAccess
            .get(player)
            .map(data -> data.recordMaterialUse(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()),
                level.getGameTime() / 1_200L,
                ArchitectConfig.recentWindowMinutes()
            ))
            .orElse(1);
        return new PendingConstruction(
            BuiltInRegistries.BLOCK.getKey(state.getBlock()),
            player.getUUID(),
            level.getGameTime() + CONSTRUCTION_DWELL_TICKS,
            itemUseCount,
            recentUseCount
        );
    }
}
