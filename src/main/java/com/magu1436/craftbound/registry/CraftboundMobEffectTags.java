package com.magu1436.craftbound.registry;

import com.magu1436.craftbound.common.CraftboundUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;

public final class CraftboundMobEffectTags {

    public static final TagKey<MobEffect> PHYSICAL_RESISTANCE =
            create("physical_resistance");

    public static final TagKey<MobEffect> ACTION_RESISTANCE =
            create("action_resistance");

    public static final TagKey<MobEffect> SENSORY_RESISTANCE =
            create("sensory_resistance");

    private CraftboundMobEffectTags() {
    }

    private static TagKey<MobEffect> create(String path) {
        return TagKey.create(
                Registries.MOB_EFFECT,
                CraftboundUtilities.createResourceLocation(path)
        );
    }
}
