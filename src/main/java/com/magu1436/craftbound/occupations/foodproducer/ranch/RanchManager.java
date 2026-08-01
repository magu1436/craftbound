package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/** 重複範囲と容量を含め、どの牧畜ブロックがどの個体を管理するかを一意に決める。 */
public final class RanchManager {

    private RanchManager() {
    }

    @Nullable
    public static RanchBlockEntity findManagingRanch(Animal animal) {
        if (!(animal.level() instanceof ServerLevel level)) {
            return null;
        }

        int minimumChunkX = SectionPos.blockToSectionCoord((int) Math.floor(animal.getX() - 9.0D));
        int maximumChunkX = SectionPos.blockToSectionCoord((int) Math.floor(animal.getX() + 7.0D));
        int minimumChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(animal.getZ() - 9.0D));
        int maximumChunkZ = SectionPos.blockToSectionCoord((int) Math.floor(animal.getZ() + 7.0D));

        RanchBlockEntity selected = null;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof RanchBlockEntity ranch)
                            || ranch.isRemoved()
                            || ranch.getTarget() == RanchTarget.UNSET
                            || !ranch.getTarget().matches(animal)
                            || !ranch.getManagementBounds().contains(animal.position())) {
                        continue;
                    }
                    if (selected == null || compareRanches(ranch, selected, animal) < 0) {
                        selected = ranch;
                    }
                }
            }
        }
        return selected;
    }

    public static List<Animal> getManagedAnimals(RanchBlockEntity ranch) {
        return getOwnedAnimals(ranch).stream()
                .limit(ranch.getManagementCapacity())
                .toList();
    }

    /** 範囲重複の所有判定後、容量制限を適用する前の全個体を返す。 */
    public static List<Animal> getOwnedAnimals(RanchBlockEntity ranch) {
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
                                && findManagingRanch(animal) == ranch
                ).stream()
                .sorted(Comparator
                        .comparingDouble((Animal animal) -> animal.distanceToSqr(center))
                        .thenComparing(Animal::getUUID))
                .toList();
    }

    public static boolean isManagedBy(Animal animal, RanchBlockEntity ranch) {
        return getManagedAnimals(ranch).contains(animal);
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
