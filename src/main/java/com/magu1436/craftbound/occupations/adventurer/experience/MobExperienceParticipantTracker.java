package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

/**
 * Mobの永続データに討伐参加者UUIDを保存する。
 */
public final class MobExperienceParticipantTracker {
    private static final String PARTICIPANTS_KEY =
        "craftbound:adventurer_experience_participants";

    private MobExperienceParticipantTracker() {
    }

    /**
     * 参加者を重複なく記録する。
     *
     * @return 新しく記録した場合はtrue
     */
    public static boolean record(
        LivingEntity entity,
        UUID participantId
    ) {
        Objects.requireNonNull(entity, "entity is null");
        Objects.requireNonNull(
            participantId,
            "participant id is null"
        );

        Set<UUID> participants = new LinkedHashSet<>(
            getParticipants(entity)
        );

        if (!participants.add(participantId)) {
            return false;
        }

        writeParticipants(entity, participants);
        return true;
    }

    /**
     * 保存済みの参加者UUIDを不変集合として返す。
     */
    public static Set<UUID> getParticipants(
        LivingEntity entity
    ) {
        Objects.requireNonNull(entity, "entity is null");

        CompoundTag persistentData = entity.getPersistentData();

        if (!persistentData.contains(PARTICIPANTS_KEY, Tag.TAG_LIST)) {
            return Set.of();
        }

        ListTag participantTags = persistentData.getList(
            PARTICIPANTS_KEY,
            Tag.TAG_INT_ARRAY
        );
        Set<UUID> participants = new LinkedHashSet<>();

        for (Tag participantTag : participantTags) {
            try {
                participants.add(NbtUtils.loadUUID(participantTag));
            } catch (IllegalArgumentException exception) {
                // 不正な要素だけを無視し、他の参加者記録は維持する。
            }
        }

        return Set.copyOf(participants);
    }

    /**
     * 保存済みの参加者UUIDを破棄する。
     */
    public static void clear(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity is null");
        entity.getPersistentData().remove(PARTICIPANTS_KEY);
    }

    private static void writeParticipants(
        LivingEntity entity,
        Set<UUID> participants
    ) {
        ListTag participantTags = new ListTag();

        for (UUID participantId : participants) {
            participantTags.add(NbtUtils.createUUID(participantId));
        }

        entity.getPersistentData().put(
            PARTICIPANTS_KEY,
            participantTags
        );
    }
}
