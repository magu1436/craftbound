package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/** 確率試験の反復を避けるため、次の有効な品質上昇抽選だけを成功させる。 */
public final class FoodCookingTestHooks {

    private static final Set<UUID> FORCED_UPGRADES = new HashSet<>();

    private FoodCookingTestHooks() {
    }

    public static void forceNextUpgrade(ServerPlayer player) {
        FORCED_UPGRADES.add(player.getUUID());
    }

    public static boolean consumeForcedUpgrade(ServerPlayer player) {
        return FORCED_UPGRADES.remove(player.getUUID());
    }
}
