package com.magu1436.craftbound.network.packet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class BlacksmithQualityTierSyncPacketTest {
    @Test
    void roundTripsQualityTierSnapshot() {
        BlacksmithQualityTierSyncPacket original = new BlacksmithQualityTierSyncPacket(List.of(
            new QualityTierDefinition("low", 0, 49, "quality.low"),
            new QualityTierDefinition("high", 50, 100, "quality.high")
        ));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        BlacksmithQualityTierSyncPacket.encode(original, buffer);

        assertEquals(original, BlacksmithQualityTierSyncPacket.decode(buffer));
    }

    @Test
    void rejectsTierCountAboveQualityRangeSize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeVarInt(102);

        assertThrows(DecoderException.class, () -> BlacksmithQualityTierSyncPacket.decode(buffer));
    }
}
