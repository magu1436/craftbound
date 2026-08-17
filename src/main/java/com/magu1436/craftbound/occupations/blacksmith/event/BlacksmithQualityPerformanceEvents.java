package com.magu1436.craftbound.occupations.blacksmith.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.quality.BlacksmithQualityResolver;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceService;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class BlacksmithQualityPerformanceEvents {
    private BlacksmithQualityPerformanceEvents() {}

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        OptionalInt quality = BlacksmithQualityResolver.resolveForPerformance(event.getItemStack());
        if (quality.isEmpty()) return;

        replaceAdditionModifiers(
            event,
            Attributes.ATTACK_DAMAGE,
            QualityPerformanceType.ATTACK_DAMAGE,
            quality.getAsInt()
        );
        replaceAdditionModifiers(
            event,
            Attributes.ARMOR,
            QualityPerformanceType.ARMOR,
            quality.getAsInt()
        );
        replaceAdditionModifiers(
            event,
            Attributes.ARMOR_TOUGHNESS,
            QualityPerformanceType.ARMOR_TOUGHNESS,
            quality.getAsInt()
        );
    }

    private static void replaceAdditionModifiers(
        ItemAttributeModifierEvent event,
        Attribute attribute,
        QualityPerformanceType type,
        int quality
    ) {
        List<AttributeModifier> originals = event.getOriginalModifiers().get(attribute).stream()
            .filter(modifier -> modifier.getOperation() == AttributeModifier.Operation.ADDITION)
            .toList();
        if (originals.isEmpty()) return;

        List<Double> originalAmounts = originals.stream()
            .map(AttributeModifier::getAmount)
            .toList();
        double originalTotal = originalAmounts.stream().mapToDouble(Double::doubleValue).sum();
        if (originalTotal == 0.0D || !Double.isFinite(originalTotal)) return;

        double adjustedTotal = QualityPerformanceService.apply(type, originalTotal, quality);
        if (!Double.isFinite(adjustedTotal)) return;
        List<Double> adjustedAmounts = distributeProportionally(originalAmounts, adjustedTotal);

        for (int index = 0; index < originals.size(); index++) {
            AttributeModifier original = originals.get(index);
            event.removeModifier(attribute, original);
            event.addModifier(attribute, new AttributeModifier(
                original.getId(),
                original.getName(),
                adjustedAmounts.get(index),
                original.getOperation()
            ));
        }
    }

    static List<Double> distributeProportionally(List<Double> originalAmounts, double adjustedTotal) {
        if (originalAmounts.isEmpty()) return List.of();
        double originalTotal = originalAmounts.stream().mapToDouble(Double::doubleValue).sum();
        if (originalTotal == 0.0D || !Double.isFinite(originalTotal) || !Double.isFinite(adjustedTotal)) {
            return List.copyOf(originalAmounts);
        }

        List<Double> adjusted = new ArrayList<>(originalAmounts.size());
        double assigned = 0.0D;
        for (int index = 0; index < originalAmounts.size() - 1; index++) {
            double amount = adjustedTotal * originalAmounts.get(index) / originalTotal;
            adjusted.add(amount);
            assigned += amount;
        }
        adjusted.add(adjustedTotal - assigned);
        return List.copyOf(adjusted);
    }
}
