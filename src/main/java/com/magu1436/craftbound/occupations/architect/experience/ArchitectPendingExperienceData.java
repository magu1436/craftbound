package com.magu1436.craftbound.occupations.architect.experience;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * オフライン中のプレイヤーへ渡す施工ポイント単位をワールドへ保存する。
 */
public final class ArchitectPendingExperienceData extends SavedData {
    private static final String DATA_NAME =
        "craftbound_architect_pending_experience";
    private static final String ENTRIES_KEY = "Entries";
    private static final String PLAYER_KEY = "Player";
    private static final String POINTS_KEY = "Points";

    private final Map<UUID, Long> pendingExperiencePoints = new HashMap<>();

    public static ArchitectPendingExperienceData get(
        MinecraftServer server
    ) {
        return server.overworld().getDataStorage().computeIfAbsent(
            ArchitectPendingExperienceData::load,
            ArchitectPendingExperienceData::new,
            DATA_NAME
        );
    }

    public void add(UUID playerId, long points) {
        if (points <= 0L) {
            return;
        }
        pendingExperiencePoints.merge(playerId, points, Long::sum);
        setDirty();
    }

    public long take(UUID playerId) {
        Long points = pendingExperiencePoints.remove(playerId);
        if (points == null) {
            return 0L;
        }
        setDirty();
        return points;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();
        for (var entry : pendingExperiencePoints.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putUUID(PLAYER_KEY, entry.getKey());
            entryTag.putLong(POINTS_KEY, entry.getValue());
            entries.add(entryTag);
        }
        tag.put(ENTRIES_KEY, entries);
        return tag;
    }

    private static ArchitectPendingExperienceData load(CompoundTag tag) {
        ArchitectPendingExperienceData data =
            new ArchitectPendingExperienceData();
        ListTag entries = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (Tag rawEntry : entries) {
            CompoundTag entry = (CompoundTag) rawEntry;
            if (entry.hasUUID(PLAYER_KEY)) {
                long points = entry.getLong(POINTS_KEY);
                if (points > 0L) {
                    data.pendingExperiencePoints.put(
                        entry.getUUID(PLAYER_KEY),
                        points
                    );
                }
            }
        }
        return data;
    }
}
