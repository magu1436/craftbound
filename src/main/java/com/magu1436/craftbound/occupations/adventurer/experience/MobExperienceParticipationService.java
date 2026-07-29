package com.magu1436.craftbound.occupations.adventurer.experience;

import java.util.Optional;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;

/**
 * 実ダメージを与えた冒険家経験値の参加者を記録する。
 */
public final class MobExperienceParticipationService {
    private static final float MINIMUM_PARTICIPATION_DAMAGE = 1.0F;

    private MobExperienceParticipationService() {
    }

    public static void recordParticipant(
        LivingDamageEvent event
    ) {
        if (
            event.getEntity().level().isClientSide()
                || event.getEntity() instanceof Player
                || !Float.isFinite(event.getAmount())
                || event.getAmount() < MINIMUM_PARTICIPATION_DAMAGE
        ) {
            return;
        }

        resolvePlayer(event.getSource()).ifPresent(player ->
            MobExperienceParticipantTracker.record(
                event.getEntity(),
                player.getUUID()
            )
        );
    }

    private static Optional<ServerPlayer> resolvePlayer(
        DamageSource damageSource
    ) {
        Entity directEntity = damageSource.getDirectEntity();

        Optional<ServerPlayer> directPlayer =
            getRealPlayer(directEntity);
        if (directPlayer.isPresent()) {
            return directPlayer;
        }

        if (directEntity instanceof Projectile projectile) {
            Optional<ServerPlayer> projectileOwner =
                getRealPlayer(projectile.getOwner());

            if (projectileOwner.isPresent()) {
                return projectileOwner;
            }
        }

        if (directEntity == null) {
            return getRealPlayer(damageSource.getEntity());
        }

        return Optional.empty();
    }

    private static Optional<ServerPlayer> getRealPlayer(
        Entity entity
    ) {
        if (
            entity instanceof ServerPlayer player
                && !(player instanceof FakePlayer)
        ) {
            return Optional.of(player);
        }

        return Optional.empty();
    }
}
