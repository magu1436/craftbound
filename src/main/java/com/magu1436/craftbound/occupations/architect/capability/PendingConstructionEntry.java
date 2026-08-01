package com.magu1436.craftbound.occupations.architect.capability;

import net.minecraft.core.BlockPos;

/**
 * チャンクから復元した施工候補とワールド座標。
 */
public record PendingConstructionEntry(
    BlockPos pos,
    PendingConstruction pending
) {
}
