package com.magu1436.craftbound.network.packet;

import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithQualityPerformanceDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceRule;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import io.netty.handler.codec.DecoderException;
import java.util.EnumMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record BlacksmithQualityPerformanceSyncPacket(
    BlacksmithQualityPerformanceDefinitions.Definition definition
) {
    public BlacksmithQualityPerformanceSyncPacket {
        definition = BlacksmithQualityPerformanceDefinitions.validatedDefinition(
            definition.defaultQuality(),
            definition.modifiers()
        );
    }

    public static void encode(BlacksmithQualityPerformanceSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.definition.defaultQuality());
        buffer.writeVarInt(packet.definition.modifiers().size());
        for (QualityPerformanceType type : QualityPerformanceType.values()) {
            QualityPerformanceRule rule = packet.definition.modifiers().get(type);
            buffer.writeEnum(type);
            buffer.writeResourceLocation(rule.evaluator());
            buffer.writeDouble(rule.base());
            buffer.writeDouble(rule.perQuality());
            buffer.writeDouble(rule.roundTo());
            buffer.writeEnum(rule.rounding());
            buffer.writeBoolean(rule.preserveDamageRatio());
        }
    }

    public static BlacksmithQualityPerformanceSyncPacket decode(FriendlyByteBuf buffer) {
        try {
            int defaultQuality = buffer.readVarInt();
            int modifierCount = buffer.readVarInt();
            if (modifierCount != QualityPerformanceType.values().length) {
                throw new DecoderException("Invalid blacksmith quality performance modifier count: " + modifierCount);
            }

            EnumMap<QualityPerformanceType, QualityPerformanceRule> modifiers =
                new EnumMap<>(QualityPerformanceType.class);
            for (int index = 0; index < modifierCount; index++) {
                QualityPerformanceType type = buffer.readEnum(QualityPerformanceType.class);
                ResourceLocation evaluator = buffer.readResourceLocation();
                QualityPerformanceRule rule = new QualityPerformanceRule(
                    evaluator,
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readEnum(QualityPerformanceRule.Rounding.class),
                    buffer.readBoolean()
                );
                if (modifiers.put(type, rule) != null) {
                    throw new DecoderException("Duplicate blacksmith quality performance modifier: " + type);
                }
            }
            return new BlacksmithQualityPerformanceSyncPacket(
                BlacksmithQualityPerformanceDefinitions.validatedDefinition(defaultQuality, modifiers)
            );
        } catch (DecoderException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new DecoderException("Invalid blacksmith quality performance definition", exception);
        }
    }

    public static void handle(
        BlacksmithQualityPerformanceSyncPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
            Dist.CLIENT,
            () -> () -> BlacksmithQualityPerformanceDefinitions.replace(packet.definition)
        ));
        context.setPacketHandled(true);
    }
}
