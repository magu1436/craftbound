package com.magu1436.craftbound.occupations.foodproducer.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** オフラインの開始者へ後から付与するFoodProducer経験値を保存する。 */
final class FoodProducerPendingExperienceData extends SavedData {

    private static final String DATA_NAME = "craftbound_food_producer_pending_experience";
    private static final String ENTRIES_TAG = "entries";
    private static final String PLAYER_TAG = "player";
    private static final String AMOUNT_TAG = "amount";

    private final Map<UUID, Integer> pendingByPlayer = new HashMap<>();

    static FoodProducerPendingExperienceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                FoodProducerPendingExperienceData::load,
                FoodProducerPendingExperienceData::new,
                DATA_NAME
        );
    }

    static FoodProducerPendingExperienceData load(CompoundTag root) {
        FoodProducerPendingExperienceData data = new FoodProducerPendingExperienceData();
        ListTag entries = root.getList(ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (Tag rawEntry : entries) {
            CompoundTag entry = (CompoundTag) rawEntry;
            if (!entry.hasUUID(PLAYER_TAG)) {
                continue;
            }
            int amount = Math.max(0, entry.getInt(AMOUNT_TAG));
            if (amount > 0) {
                data.pendingByPlayer.put(entry.getUUID(PLAYER_TAG), amount);
            }
        }
        return data;
    }

    int get(UUID playerId) {
        return pendingByPlayer.getOrDefault(playerId, 0);
    }

    int add(UUID playerId, int amount) {
        if (amount <= 0) {
            return get(playerId);
        }
        int current = get(playerId);
        int updated = (int) Math.min(Integer.MAX_VALUE, current + (long) amount);
        pendingByPlayer.put(playerId, updated);
        setDirty();
        return updated;
    }

    void remove(UUID playerId) {
        if (pendingByPlayer.remove(playerId) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        pendingByPlayer.forEach((playerId, amount) -> {
            if (amount <= 0) {
                return;
            }
            CompoundTag entry = new CompoundTag();
            entry.putUUID(PLAYER_TAG, playerId);
            entry.putInt(AMOUNT_TAG, amount);
            entries.add(entry);
        });
        root.put(ENTRIES_TAG, entries);
        return root;
    }
}
