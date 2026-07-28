package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class CraftboundItemTags {

    public static final TagKey<Item> SHIELD_FOOTWORK_ITEMS =
        create("adventurer/shield_footwork_items");

    public static final TagKey<Item> SHIELD_FOOTWORK_EXCLUDED_ITEMS =
        create("adventurer/shield_footwork_excluded_items");

    private CraftboundItemTags() {
    }

    private static TagKey<Item> create(String path) {
        return TagKey.create(
            Registries.ITEM,
            CraftboundUtilities.createResourceLocation(path)
        );
    }
}
