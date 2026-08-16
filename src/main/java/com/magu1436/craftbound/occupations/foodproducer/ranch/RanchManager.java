package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** 安定登録と重複範囲を含め、家畜と牧畜ブロックの対応を解決する。 */
public final class RanchManager {

    private RanchManager() {
    }

    /** 登録関係が有効で、かつ現在範囲内にいる場合だけ恩恵元を返す。 */
    @Nullable
    public static RanchBlockEntity findManagingRanch(Animal animal) {
        RanchBlockEntity ranch = findAssignedRanch(animal);
        return ranch != null
                && animal.level() == ranch.getLevel()
                && ranch.getManagementBounds().contains(animal.position())
                && ranch.getTarget().matches(animal)
                ? ranch
                : null;
    }

    /** 範囲外猶予中を含む、保存済みの登録先を返す。未読込チャンクは変更しない。 */
    @Nullable
    public static RanchBlockEntity findAssignedRanch(Animal animal) {
        if (!(animal.level() instanceof ServerLevel currentLevel)) {
            return null;
        }
        RanchAnimalData.Assignment assignment = RanchAnimalData.getAssignment(animal);
        if (assignment == null) {
            return null;
        }

        ServerLevel level = null;
        for (ServerLevel candidateLevel : currentLevel.getServer().getAllLevels()) {
            if (assignment.dimension().equals(candidateLevel.dimension().location().toString())) {
                level = candidateLevel;
                break;
            }
        }
        if (level == null) {
            return null;
        }

        BlockPos position = assignment.position();
        LevelChunk chunk = level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ())
        );
        if (chunk == null) {
            return null;
        }

        BlockEntity blockEntity = chunk.getBlockEntity(position);
        if (blockEntity instanceof RanchBlockEntity ranch
                && !ranch.isRemoved()
                && ranch.hasRegistration(animal.getUUID())) {
            return ranch;
        }

        RanchAnimalData.clearAssignment(animal);
        return null;
    }

    public static List<Animal> getManagedAnimals(RanchBlockEntity ranch) {
        return ranch.getActiveManagedAnimals();
    }

    /** 対象種かつ現在範囲内にいる全個体。新規登録時だけ距離・UUID順を使用する。 */
    public static List<Animal> getAnimalsInRange(RanchBlockEntity ranch) {
        if (!(ranch.getLevel() instanceof ServerLevel level) || ranch.getTarget() == RanchTarget.UNSET) {
            return List.of();
        }

        Vec3 center = Vec3.atCenterOf(ranch.getBlockPos());
        return level.getEntitiesOfClass(
                        Animal.class,
                        ranch.getManagementBounds(),
                        animal -> animal.isAlive()
                                && ranch.getTarget().matches(animal)
                                && ranch.getManagementBounds().contains(animal.position())
                ).stream()
                .sorted(Comparator
                        .comparingDouble((Animal animal) -> animal.distanceToSqr(center))
                        .thenComparing(Animal::getUUID))
                .toList();
    }

    public static boolean isManagedBy(Animal animal, RanchBlockEntity ranch) {
        return findManagingRanch(animal) == ranch;
    }

    /** 重複範囲では、空きのある最寄り牧畜ブロックだけが新規登録できる。 */
    public static boolean mayRegister(Animal animal, RanchBlockEntity candidate) {
        RanchBlockEntity assigned = findAssignedRanch(animal);
        if (assigned != null) {
            return assigned == candidate;
        }

        RanchBlockEntity preferred = null;
        for (RanchBlockEntity ranch : nearbyRanches(animal)) {
            if (ranch.getTarget() == RanchTarget.UNSET
                    || !ranch.getTarget().matches(animal)
                    || !ranch.getManagementBounds().contains(animal.position())
                    || !ranch.hasRegistrationSpace()) {
                continue;
            }
            if (preferred == null || compareRanches(ranch, preferred, animal) < 0) {
                preferred = ranch;
            }
        }
        return preferred == candidate;
    }

    @Nullable
    public static Animal findLoadedAnimal(RanchBlockEntity ranch, java.util.UUID animalId) {
        if (!(ranch.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        Entity entity = level.getEntity(animalId);
        if (entity instanceof Animal animal) {
            return animal;
        }
        for (ServerLevel otherLevel : level.getServer().getAllLevels()) {
            if (otherLevel == level) {
                continue;
            }
            entity = otherLevel.getEntity(animalId);
            if (entity instanceof Animal animal) {
                return animal;
            }
        }
        return null;
    }

    private static List<RanchBlockEntity> nearbyRanches(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return List.of();
        }

        int minimumChunkX = SectionPos.blockToSectionCoord((int) Math.floor(animal.getX() - 9.0D));
        int maximumChunkX = SectionPos.blockToSectionCoord((int) Math.floor(animal.getX() + 7.0D));
        int minimumChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(animal.getZ() - 9.0D));
        int maximumChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(animal.getZ() + 7.0D));
        java.util.ArrayList<RanchBlockEntity> ranches = new java.util.ArrayList<>();
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof RanchBlockEntity ranch && !ranch.isRemoved()) {
                        ranches.add(ranch);
                    }
                }
            }
        }
        return ranches;
    }

    private static int compareRanches(
            RanchBlockEntity first,
            RanchBlockEntity second,
            Animal animal
    ) {
        double firstDistance = animal.distanceToSqr(Vec3.atCenterOf(first.getBlockPos()));
        double secondDistance = animal.distanceToSqr(Vec3.atCenterOf(second.getBlockPos()));
        int distanceResult = Double.compare(firstDistance, secondDistance);
        return distanceResult != 0
                ? distanceResult
                : Long.compare(first.getBlockPos().asLong(), second.getBlockPos().asLong());
    }
}
