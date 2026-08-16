package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 牧畜ブロックの安定した登録枠を保持する、Minecraft状態に依存しない小さな台帳。
 * 候補の距離順は新規登録時だけ使い、登録済みUUIDの順番は移動で変えない。
 */
final class RanchRegistrationRoster {

    static final long INSIDE = -1L;

    private final LinkedHashMap<UUID, Long> registrations = new LinkedHashMap<>();

    boolean register(UUID animalId, int capacity) {
        if (registrations.containsKey(animalId)) {
            return true;
        }
        if (registrations.size() >= Math.max(0, capacity)) {
            return false;
        }
        registrations.put(animalId, INSIDE);
        return true;
    }

    void load(UUID animalId, long outsideSince) {
        registrations.put(animalId, Math.max(INSIDE, outsideSince));
    }

    boolean contains(UUID animalId) {
        return registrations.containsKey(animalId);
    }

    int size() {
        return registrations.size();
    }

    List<UUID> ids() {
        return List.copyOf(registrations.keySet());
    }

    List<Map.Entry<UUID, Long>> entries() {
        return new ArrayList<>(registrations.entrySet());
    }

    long outsideSince(UUID animalId) {
        return registrations.getOrDefault(animalId, INSIDE);
    }

    void markInside(UUID animalId) {
        if (registrations.containsKey(animalId)) {
            registrations.put(animalId, INSIDE);
        }
    }

    void markOutside(UUID animalId, long gameTime) {
        if (registrations.getOrDefault(animalId, INSIDE) == INSIDE) {
            registrations.put(animalId, Math.max(0L, gameTime));
        }
    }

    boolean graceExpired(UUID animalId, long gameTime, long graceTicks) {
        long outsideSince = registrations.getOrDefault(animalId, INSIDE);
        return outsideSince != INSIDE && gameTime - outsideSince >= graceTicks;
    }

    boolean release(UUID animalId) {
        return registrations.remove(animalId) != null;
    }

    void clear() {
        registrations.clear();
    }
}
