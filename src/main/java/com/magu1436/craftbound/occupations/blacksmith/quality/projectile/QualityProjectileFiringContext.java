package com.magu1436.craftbound.occupations.blacksmith.quality.projectile;

import com.magu1436.craftbound.common.quality.QualityState;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class QualityProjectileFiringContext {
    private static final Map<UUID, Entry> ENTRIES = new ConcurrentHashMap<>();

    private QualityProjectileFiringContext() {}

    public static void record(UUID shooterId, int quality, long gameTime) {
        if (!QualityState.isValidQuality(quality)) {
            throw new IllegalArgumentException("quality is outside the valid range: " + quality);
        }
        ENTRIES.put(shooterId, new Entry(quality, gameTime));
    }

    public static OptionalInt find(UUID shooterId, long gameTime) {
        Entry entry = ENTRIES.get(shooterId);
        if (entry == null) return OptionalInt.empty();
        if (entry.gameTime() != gameTime) {
            ENTRIES.remove(shooterId, entry);
            return OptionalInt.empty();
        }
        return OptionalInt.of(entry.quality());
    }

    public static void clear(UUID shooterId) {
        ENTRIES.remove(shooterId);
    }

    static void clearAll() {
        ENTRIES.clear();
    }

    private record Entry(int quality, long gameTime) {}
}
