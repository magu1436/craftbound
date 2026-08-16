package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;

/** 牧畜管理に必要な個体単位の状態を、移動後も残るEntity NBTへ保存する。 */
public final class RanchAnimalData {

    public static final int CHILD_GROWTH_TICKS = 60 * 60 * 20;
    public static final int ADULT_FEED_INTERVAL_TICKS = 20 * 60 * 20;
    public static final int CHILD_FEED_INTERVAL_TICKS = 10 * 60 * 20;

    private static final String EVER_MANAGED = "craftbound_ranch_ever_managed";
    private static final String REMAINING_GROWTH = "craftbound_ranch_remaining_growth";
    private static final String FED = "craftbound_ranch_fed";
    private static final String NEXT_FEED_TIME = "craftbound_ranch_next_feed_time";
    private static final String LAST_FEEDER = "craftbound_ranch_last_feeder";
    private static final String LAST_FEED_TIME = "craftbound_ranch_last_feed_time";
    private static final String LAST_BREEDING_RANK = "craftbound_ranch_last_breeding_rank";
    private static final String ASSIGNED_DIMENSION = "craftbound_ranch_assigned_dimension";
    private static final String ASSIGNED_POSITION = "craftbound_ranch_assigned_position";

    private RanchAnimalData() {
    }

    public static void ensureManaged(AgeableMob animal) {
        CompoundTag data = animal.getPersistentData();
        if (data.getBoolean(EVER_MANAGED)) {
            return;
        }
        data.putBoolean(EVER_MANAGED, true);
        if (animal.isBaby()) {
            double vanillaRemaining = Math.min(1.0D, Math.max(0.0D,
                    -animal.getAge() / (double) -AgeableMob.BABY_START_AGE));
            setRemainingGrowth(animal, Math.max(1, (int) Math.ceil(CHILD_GROWTH_TICKS * vanillaRemaining)));
        }
    }

    public static void initializeNewborn(AgeableMob animal) {
        animal.getPersistentData().putBoolean(EVER_MANAGED, true);
        setRemainingGrowth(animal, CHILD_GROWTH_TICKS);
        setFed(animal, false);
        clearNextFeedTime(animal);
    }

    public static boolean wasEverManaged(AgeableMob animal) {
        return animal.getPersistentData().getBoolean(EVER_MANAGED);
    }

    public static int getRemainingGrowth(AgeableMob animal) {
        return Math.max(0, animal.getPersistentData().getInt(REMAINING_GROWTH));
    }

    public static void setRemainingGrowth(AgeableMob animal, int ticks) {
        int remaining = Math.max(0, ticks);
        animal.getPersistentData().putInt(REMAINING_GROWTH, remaining);
        animal.setAge(remaining == 0 ? 0 : -remaining);
    }

    public static boolean isFed(Animal animal) {
        return animal.getPersistentData().getBoolean(FED);
    }

    public static void setFed(AgeableMob animal, boolean fed) {
        animal.getPersistentData().putBoolean(FED, fed);
    }

    public static boolean hasNextFeedTime(Animal animal) {
        return animal.getPersistentData().contains(NEXT_FEED_TIME);
    }

    public static long getNextFeedTime(Animal animal) {
        return animal.getPersistentData().getLong(NEXT_FEED_TIME);
    }

    public static void setNextFeedTime(Animal animal, long gameTime) {
        animal.getPersistentData().putLong(NEXT_FEED_TIME, gameTime);
    }

    public static void clearNextFeedTime(AgeableMob animal) {
        animal.getPersistentData().remove(NEXT_FEED_TIME);
    }

    public static void rememberDirectFeeder(Animal animal, UUID playerId, long gameTime, int rank) {
        CompoundTag data = animal.getPersistentData();
        data.putUUID(LAST_FEEDER, playerId);
        data.putLong(LAST_FEED_TIME, gameTime);
        data.putInt(LAST_BREEDING_RANK, Math.max(0, Math.min(5, rank)));
    }

    @Nullable
    public static UUID getLastFeeder(Animal animal) {
        CompoundTag data = animal.getPersistentData();
        return data.hasUUID(LAST_FEEDER) ? data.getUUID(LAST_FEEDER) : null;
    }

    public static long getLastDirectFeedTime(Animal animal) {
        return animal.getPersistentData().getLong(LAST_FEED_TIME);
    }

    public static int getLastBreedingRank(Animal animal) {
        return Math.max(0, Math.min(5, animal.getPersistentData().getInt(LAST_BREEDING_RANK)));
    }

    public static void assignTo(Animal animal, ServerLevel level, BlockPos ranchPosition) {
        CompoundTag data = animal.getPersistentData();
        data.putString(ASSIGNED_DIMENSION, level.dimension().location().toString());
        data.putLong(ASSIGNED_POSITION, ranchPosition.asLong());
    }

    public static boolean isAssignedTo(Animal animal, ServerLevel level, BlockPos ranchPosition) {
        Assignment assignment = getAssignment(animal);
        return assignment != null
                && assignment.dimension().equals(level.dimension().location().toString())
                && assignment.position().equals(ranchPosition);
    }

    @Nullable
    public static Assignment getAssignment(Animal animal) {
        CompoundTag data = animal.getPersistentData();
        if (!data.contains(ASSIGNED_DIMENSION) || !data.contains(ASSIGNED_POSITION)) {
            return null;
        }
        return new Assignment(
                data.getString(ASSIGNED_DIMENSION),
                BlockPos.of(data.getLong(ASSIGNED_POSITION))
        );
    }

    public static void clearAssignment(Animal animal) {
        CompoundTag data = animal.getPersistentData();
        data.remove(ASSIGNED_DIMENSION);
        data.remove(ASSIGNED_POSITION);
    }

    public static void clearAssignmentIfMatches(Animal animal, ServerLevel level, BlockPos ranchPosition) {
        if (isAssignedTo(animal, level, ranchPosition)) {
            clearAssignment(animal);
        }
    }

    public record Assignment(String dimension, BlockPos position) {
    }
}
