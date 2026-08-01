package com.magu1436.craftbound.occupations.architect.capability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

class PlayerPlacedBlockDataImplTest {

    @Test
    void addAndRemoveAreIdempotent() {
        PlayerPlacedBlockDataImpl data =
            new PlayerPlacedBlockDataImpl();
        BlockPos pos = new BlockPos(10, 64, -20);

        assertTrue(data.add(pos));
        assertFalse(data.add(pos));
        assertTrue(data.contains(pos));
        assertTrue(data.remove(pos));
        assertFalse(data.remove(pos));
        assertFalse(data.contains(pos));
    }

    @Test
    void persistentDataRoundTripsVersionAndPositions() {
        PlayerPlacedBlockDataImpl original =
            new PlayerPlacedBlockDataImpl();
        original.add(new BlockPos(1, 2, 3));
        original.add(new BlockPos(-4, 5, 6));

        CompoundTag tag = original.savePersistentData();
        PlayerPlacedBlockDataImpl restored =
            new PlayerPlacedBlockDataImpl();
        restored.loadPersistentData(tag);

        long[] expected = original.getPositions();
        long[] actual = restored.getPositions();
        Arrays.sort(expected);
        Arrays.sort(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    void removeAllReturnsNumberOfChangedPositions() {
        PlayerPlacedBlockDataImpl data =
            new PlayerPlacedBlockDataImpl();
        BlockPos first = new BlockPos(1, 2, 3);
        BlockPos second = new BlockPos(4, 5, 6);
        data.add(first);
        data.add(second);

        int removed = data.removeAll(new LongArrayList(new long[] {
            first.asLong(),
            new BlockPos(7, 8, 9).asLong()
        }));

        assertEquals(1, removed);
        assertFalse(data.contains(first));
        assertTrue(data.contains(second));
    }
}
