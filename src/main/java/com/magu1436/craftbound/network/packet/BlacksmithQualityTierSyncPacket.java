package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.common.quality.QualityState;
import com.magu1436.craftbound.occupations.blacksmith.client.ClientBlacksmithQualityTierDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityTierDefinition;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record BlacksmithQualityTierSyncPacket(List<QualityTierDefinition> tiers) {
    private static final int MAX_TIER_COUNT = QualityState.MAX_QUALITY - QualityState.MIN_QUALITY + 1;
    private static final int MAX_STRING_LENGTH = 256;

    public BlacksmithQualityTierSyncPacket {
        tiers = List.copyOf(tiers);
    }

    public static void encode(BlacksmithQualityTierSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.tiers.size());
        for (QualityTierDefinition tier : packet.tiers) {
            buffer.writeUtf(tier.id(), MAX_STRING_LENGTH);
            buffer.writeVarInt(tier.min());
            buffer.writeVarInt(tier.max());
            buffer.writeUtf(tier.translationKey(), MAX_STRING_LENGTH);
        }
    }

    public static BlacksmithQualityTierSyncPacket decode(FriendlyByteBuf buffer) {
        int tierCount = buffer.readVarInt();
        if (tierCount < 1 || tierCount > MAX_TIER_COUNT) {
            throw new DecoderException("Invalid blacksmith quality tier count: " + tierCount);
        }

        List<QualityTierDefinition> tiers = new ArrayList<>(tierCount);
        for (int index = 0; index < tierCount; index++) {
            tiers.add(new QualityTierDefinition(
                buffer.readUtf(MAX_STRING_LENGTH),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(MAX_STRING_LENGTH)
            ));
        }
        validate(tiers);
        tiers.sort(Comparator.comparingInt(QualityTierDefinition::min));
        return new BlacksmithQualityTierSyncPacket(tiers);
    }

    public static void handle(
        BlacksmithQualityTierSyncPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> ClientBlacksmithQualityTierDefinitions.replace(packet.tiers)
        ));
        context.setPacketHandled(true);
    }

    private static void validate(List<QualityTierDefinition> tiers) {
        List<QualityTierDefinition> sorted = new ArrayList<>(tiers);
        sorted.sort(Comparator.comparingInt(QualityTierDefinition::min));
        Set<String> ids = new HashSet<>();
        int expectedMin = QualityState.MIN_QUALITY;

        for (QualityTierDefinition tier : sorted) {
            if (tier.id().isBlank() || !ids.add(tier.id())) {
                throw new DecoderException("Blacksmith quality tier IDs must be non-blank and unique");
            }
            if (tier.translationKey().isBlank()) {
                throw new DecoderException("Blacksmith quality tier translation keys must not be blank");
            }
            if (!QualityState.isValidQuality(tier.min())
                || !QualityState.isValidQuality(tier.max())
                || tier.min() > tier.max()
                || tier.min() != expectedMin) {
                throw new DecoderException("Blacksmith quality tiers must completely partition the quality range");
            }
            expectedMin = tier.max() + 1;
        }
        if (expectedMin != QualityState.MAX_QUALITY + 1) {
            throw new DecoderException("Blacksmith quality tiers must end at " + QualityState.MAX_QUALITY);
        }
    }
}
