package com.magu1436.craftbound.occupations.architect.capability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

class ConstructionChunkDataImplTest {

    private static final int MIN_BUILD_HEIGHT = -64;

    @Test
    void playerPlacedStateChangesAreIdempotent() {
        ConstructionChunkDataImpl data =
            new ConstructionChunkDataImpl(MIN_BUILD_HEIGHT);
        BlockPos pos = new BlockPos(10, 64, -20);

        assertTrue(data.markPlayerPlaced(pos));
        assertFalse(data.markPlayerPlaced(pos));
        assertTrue(data.isPlayerPlaced(pos));
        assertTrue(data.clearPlayerPlaced(pos));
        assertFalse(data.clearPlayerPlaced(pos));
        assertFalse(data.isPlayerPlaced(pos));
    }

    @Test
    void persistentDataRoundTripsVersionAndPositions() {
        ConstructionChunkDataImpl original =
            new ConstructionChunkDataImpl(MIN_BUILD_HEIGHT);
        original.markPlayerPlaced(new BlockPos(1, 2, 3));
        original.markPlayerPlaced(new BlockPos(-4, 5, 6));

        CompoundTag tag = original.savePersistentData();
        ConstructionChunkDataImpl restored =
            new ConstructionChunkDataImpl(MIN_BUILD_HEIGHT);
        restored.loadPersistentData(tag);

        int[] expected = original.getPlayerPlacedPositions();
        int[] actual = restored.getPlayerPlacedPositions();
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    void clearingPlayerPlacedStateKeepsXpRewardedState() {
        ConstructionChunkDataImpl data =
            new ConstructionChunkDataImpl(MIN_BUILD_HEIGHT);
        BlockPos pos = new BlockPos(1, 2, 3);

        data.markPlayerPlaced(pos);
        data.tryMarkXpRewarded(pos);
        data.clearPlayerPlaced(pos);

        assertFalse(data.isPlayerPlaced(pos));
        assertTrue(data.isXpRewarded(pos));
    }
}
