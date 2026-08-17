package com.magu1436.craftbound.network.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityPerformanceDefinitions;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class BlacksmithQualityPerformanceSyncPacketTest {
    @Test
    void roundTripsQualityPerformanceSnapshot() {
        BlacksmithQualityPerformanceSyncPacket original = new BlacksmithQualityPerformanceSyncPacket(
            BlacksmithQualityPerformanceDefinitions.current()
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        BlacksmithQualityPerformanceSyncPacket.encode(original, buffer);

        assertEquals(original, BlacksmithQualityPerformanceSyncPacket.decode(buffer));
    }

    @Test
    void rejectsIncompleteModifierSnapshot() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(30);
        buffer.writeVarInt(5);

        assertThrows(DecoderException.class,
            () -> BlacksmithQualityPerformanceSyncPacket.decode(buffer));
    }

    @Test
    void rejectsInvalidDefaultQualityBeforeReadingModifiers() {
        FriendlyByteBuf source = new FriendlyByteBuf(Unpooled.buffer());
        BlacksmithQualityPerformanceSyncPacket.encode(
            new BlacksmithQualityPerformanceSyncPacket(BlacksmithQualityPerformanceDefinitions.current()),
            source
        );
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        source.readVarInt();
        buffer.writeVarInt(101);
        buffer.writeBytes(source);

        assertThrows(DecoderException.class,
            () -> BlacksmithQualityPerformanceSyncPacket.decode(buffer));
    }
}
