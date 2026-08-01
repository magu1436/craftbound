package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Craftboundが発動条件に使用するブロックタグ。
 */
public final class CraftboundBlockTags {
    public static final TagKey<Block> ARCHITECT_DEMOLITION_BLACKLIST =
        TagKey.create(
            Registries.BLOCK,
            CraftboundUtilities.createResourceLocation(
                "architect_demolition_blacklist"
            )
            );

    public static final TagKey<Block> ARCHITECT_XP_BLACKLIST =
        TagKey.create(
            Registries.BLOCK,
            CraftboundUtilities.createResourceLocation(
                "architect_xp_blacklist"
            )
        );

    public static final TagKey<Block> ARCHITECT_XP_WHITELIST =
        TagKey.create(
            Registries.BLOCK,
            CraftboundUtilities.createResourceLocation(
                "architect_xp_whitelist"
            )
        );

    public static final TagKey<Block> ARCHITECT_XP_REDUCED =
        TagKey.create(
            Registries.BLOCK,
            CraftboundUtilities.createResourceLocation(
                "architect_xp_reduced"
            )
        );

    private CraftboundBlockTags() {
    }
}
