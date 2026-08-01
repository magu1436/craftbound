package com.magu1436.craftbound.occupations.architect.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/**
 * 建築家プレイヤーデータの実装。
 */
public final class ArchitectData implements IArchitectData {
    private static final String POINT_UNIT_REMAINDER_KEY =
        "PointUnitRemainder";
    private static final String RECENT_USAGE_KEY = "RecentMaterialUsage";
    private static final String MATERIAL_KEY = "Material";
    private static final String BUCKETS_KEY = "MinuteBuckets";
    private static final String LAST_UPDATED_MINUTE_KEY = "LastUpdatedMinute";

    private final Map<ResourceLocation, RecentUsageWindow> recentUsage =
        new HashMap<>();
    private long pointUnitRemainder;

    @Override
    public int recordMaterialUse(
        ResourceLocation materialId,
        long currentMinute,
        int windowMinutes
    ) {
        Objects.requireNonNull(materialId, "material id is null");
        if (windowMinutes <= 0) {
            throw new IllegalArgumentException(
                "window minutes must be greater than 0"
            );
        }
        return recentUsage
            .computeIfAbsent(
                materialId,
                ignored -> new RecentUsageWindow(
                    windowMinutes,
                    currentMinute
                )
            )
            .record(currentMinute, windowMinutes);
    }

    @Override
    public long addPointUnits(
        long pointUnits,
        long pointUnitsPerExperience
    ) {
        if (pointUnits <= 0L) {
            return 0L;
        }
        if (pointUnitsPerExperience <= 0L) {
            throw new IllegalArgumentException(
                "point units per experience must be greater than 0"
            );
        }
        long total = Math.addExact(pointUnitRemainder, pointUnits);
        long experience = total / pointUnitsPerExperience;
        pointUnitRemainder = total % pointUnitsPerExperience;
        return experience;
    }

    @Override
    public long getPointUnitRemainder() {
        return pointUnitRemainder;
    }

    @Override
    public CompoundTag savePersistentData() {
        CompoundTag tag = new CompoundTag();
        tag.putLong(POINT_UNIT_REMAINDER_KEY, pointUnitRemainder);
        ListTag usageEntries = new ListTag();
        for (var entry : recentUsage.entrySet()) {
            CompoundTag usageTag = new CompoundTag();
            usageTag.putString(MATERIAL_KEY, entry.getKey().toString());
            usageTag.putLongArray(
                BUCKETS_KEY,
                entry.getValue().getMinuteBuckets()
            );
            usageTag.putLong(
                LAST_UPDATED_MINUTE_KEY,
                entry.getValue().getLastUpdatedMinute()
            );
            usageEntries.add(usageTag);
        }
        tag.put(RECENT_USAGE_KEY, usageEntries);
        return tag;
    }

    @Override
    public void loadPersistentData(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag is null");
        pointUnitRemainder = Math.max(
            0L,
            tag.getLong(POINT_UNIT_REMAINDER_KEY)
        );
        recentUsage.clear();
        for (Tag rawUsage : tag.getList(
            RECENT_USAGE_KEY,
            Tag.TAG_COMPOUND
        )) {
            CompoundTag usageTag = (CompoundTag) rawUsage;
            ResourceLocation materialId = ResourceLocation.tryParse(
                usageTag.getString(MATERIAL_KEY)
            );
            long[] buckets = usageTag.getLongArray(BUCKETS_KEY);
            if (materialId != null && buckets.length > 0) {
                recentUsage.put(
                    materialId,
                    new RecentUsageWindow(
                        buckets,
                        usageTag.getLong(LAST_UPDATED_MINUTE_KEY)
                    )
                );
            }
        }
    }

    @Override
    public void copyOnDeathFrom(IArchitectData original) {
        Objects.requireNonNull(original, "original is null");
        loadPersistentData(original.savePersistentData());
    }
}
