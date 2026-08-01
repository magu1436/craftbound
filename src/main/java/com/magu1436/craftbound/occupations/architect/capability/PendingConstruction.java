package com.magu1436.craftbound.occupations.architect.capability;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * ブロックの残存確認を待つ施工候補。
 */
public record PendingConstruction(
    ResourceLocation expectedBlockId,
    UUID playerId,
    long matureAtGameTime,
    int lifetimeUseCountSnapshot
) {
}
