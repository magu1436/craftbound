package com.magu1436.craftbound.occupations.blacksmith.crucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

class CrucibleStateCodecTest {

    @Test
    void absentTagReadsAsEmptyState() {
        assertEquals(
            Optional.of(CrucibleState.empty()),
            CrucibleStateCodec.read(new CompoundTag())
        );
    }

    @Test
    void nonEmptyStateRoundTrips() {
        CompoundTag root = new CompoundTag();
        CrucibleState expected = heatedState();

        CrucibleStateCodec.write(root, expected);

        assertEquals(Optional.of(expected), CrucibleStateCodec.read(root));
    }

    @Test
    void emptyStateRemovesEntireCrucibleTag() {
        CompoundTag root = new CompoundTag();
        CrucibleStateCodec.write(root, heatedState());

        CrucibleStateCodec.write(root, CrucibleState.empty());

        assertFalse(root.contains(CrucibleStateCodec.ROOT_KEY));
    }

    @Test
    void unsupportedVersionIsRejectedWithoutMutation() {
        CompoundTag root = rootWithValidUnheatedTag();
        root.getCompound(CrucibleStateCodec.ROOT_KEY)
            .putInt(CrucibleStateCodec.VERSION_KEY, 99);
        CompoundTag before = root.copy();

        assertTrue(CrucibleStateCodec.read(root).isEmpty());
        assertEquals(before, root);
    }

    @Test
    void unknownProcessStateIsRejected() {
        CompoundTag root = rootWithValidUnheatedTag();
        root.getCompound(CrucibleStateCodec.ROOT_KEY).putString(
            CrucibleStateCodec.PROCESS_STATE_KEY,
            "MOLTEN"
        );

        assertTrue(CrucibleStateCodec.read(root).isEmpty());
    }

    @Test
    void amountAboveCapacityIsRejected() {
        CompoundTag root = rootWithValidUnheatedTag();
        root.getCompound(CrucibleStateCodec.ROOT_KEY).putInt(
            CrucibleStateCodec.AMOUNT_KEY,
            CrucibleState.MAX_CAPACITY + 1
        );

        assertTrue(CrucibleStateCodec.read(root).isEmpty());
    }

    private static CompoundTag rootWithValidUnheatedTag() {
        CompoundTag root = new CompoundTag();
        CrucibleStateCodec.write(root, new CrucibleState(
            CrucibleState.CURRENT_VERSION,
            id("craftbound:iron"),
            1,
            0L,
            CrucibleProcessState.UNHEATED
        ));
        return root;
    }

    private static CrucibleState heatedState() {
        return new CrucibleState(
            CrucibleState.CURRENT_VERSION,
            id("craftbound:iron"),
            8,
            120L,
            CrucibleProcessState.HEATED
        );
    }

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }
}
