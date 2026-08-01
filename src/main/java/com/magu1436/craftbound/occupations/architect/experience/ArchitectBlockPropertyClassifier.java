package com.magu1436.craftbound.occupations.architect.experience;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.magu1436.craftbound.occupations.architect.ArchitectConfig;
import com.magu1436.craftbound.registry.CraftboundBlockTags;

/**
 * 明示タグと硬度から建築経験値のブロック特性倍率を決定する。
 */
public final class ArchitectBlockPropertyClassifier {

    private ArchitectBlockPropertyClassifier() {
    }

    public static int resolveRate(
        ServerLevel level,
        BlockPos pos,
        BlockState state
    ) {
        if (state.is(CraftboundBlockTags.ARCHITECT_XP_BLACKLIST)) {
            return 0;
        }
        if (state.is(CraftboundBlockTags.ARCHITECT_XP_WHITELIST)) {
            return ArchitectConfig.fullRate();
        }
        if (state.is(CraftboundBlockTags.ARCHITECT_XP_REDUCED)) {
            return ArchitectConfig.propertyReducedRate();
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness <= 0.0F) {
            return 0;
        }
        if (hardness < ArchitectConfig.lowHardnessThreshold()) {
            return ArchitectConfig.propertyReducedRate();
        }
        return ArchitectConfig.fullRate();
    }
}
