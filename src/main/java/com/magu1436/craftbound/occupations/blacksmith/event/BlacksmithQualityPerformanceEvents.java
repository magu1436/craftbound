package com.magu1436.craftbound.occupations.blacksmith.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.quality.BlacksmithQualityResolver;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceService;
import com.magu1436.craftbound.occupations.blacksmith.quality.QualityPerformanceType;
import com.magu1436.craftbound.occupations.blacksmith.quality.projectile.QualityProjectileFiringContext;
import com.magu1436.craftbound.occupations.blacksmith.quality.projectile.QualityProjectileImpactContext;
import com.magu1436.craftbound.occupations.blacksmith.quality.projectile.QualityProjectileStateService;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recordFiringQuality(player, event.getBow());
    }

    @SubscribeEvent
    public static void onLivingGetProjectile(LivingGetProjectileEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recordFiringQuality(player, event.getProjectileWeaponItemStack());
    }

    @SubscribeEvent
    public static void onProjectileJoinLevel(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()
            || !(event.getLevel() instanceof ServerLevel level)
            || !(event.getEntity() instanceof Projectile projectile)
            || !(projectile.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        QualityProjectileFiringContext.find(owner.getUUID(), level.getGameTime())
            .ifPresent(quality -> QualityProjectileStateService.setQuality(projectile, quality));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        findDamageProjectile(event.getSource().getDirectEntity())
            .flatMap(projectile -> {
                OptionalInt quality = QualityProjectileStateService.readQuality(projectile);
                return quality.isPresent()
                    ? java.util.Optional.of(quality.getAsInt())
                    : java.util.Optional.empty();
            })
            .ifPresent(quality -> event.setAmount(QualityPerformanceService.apply(
                QualityPerformanceType.PROJECTILE_DAMAGE,
                event.getAmount(),
                quality
            )));
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        QualityProjectileFiringContext.clear(event.getEntity().getUUID());
    }

    private static void recordFiringQuality(ServerPlayer player, ItemStack weapon) {
        BlacksmithQualityResolver.resolveForPerformance(weapon).ifPresent(quality ->
            QualityProjectileFiringContext.record(
                player.getUUID(),
                quality,
                player.level().getGameTime()
            )
        );
    }

    private static java.util.Optional<Projectile> findDamageProjectile(Entity directEntity) {
        if (directEntity instanceof Projectile directProjectile
            && QualityProjectileStateService.readQuality(directProjectile).isPresent()) {
            return java.util.Optional.of(directProjectile);
        }
        return QualityProjectileImpactContext.current()
            .filter(projectile -> QualityProjectileStateService.readQuality(projectile).isPresent());
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
