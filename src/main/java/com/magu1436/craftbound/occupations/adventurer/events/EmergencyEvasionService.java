package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.Objects;

import com.magu1436.craftbound.occupations.adventurer.AdventurerConfig;
import com.magu1436.craftbound.occupations.adventurer.capability.IAdventurerData;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/**
 * 緊急回避の使用条件判定と速度適用を処理する。
 */
public final class EmergencyEvasionService {
    private static final double MIN_HORIZONTAL_DIRECTION_LENGTH_SQUARED =
        1.0E-8D;

    private EmergencyEvasionService() {
    }

    /**
     * 緊急回避の使用を試みる。
     *
     * @param player 使用プレイヤー
     * @return 回避が成立した場合は {@code true}
     */
    public static boolean tryEvade(ServerPlayer player) {
        Objects.requireNonNull(player, "player is null");

        if (!canEvadeFromCurrentMovementState(player)) {
            return false;
        }

        MinecraftServer server = Objects.requireNonNull(
            player.getServer(),
            "server is null"
        );
        long currentTick = server.overworld().getGameTime();

        return player
            .getCapability(CraftboundCapabilities.ADVENTURER_DATA)
            .map(data -> tryEvade(player, data, currentTick))
            .orElse(false);
    }

    private static boolean tryEvade(
        ServerPlayer player,
        IAdventurerData data,
        long currentTick
    ) {
        int stage = data.getEmergencyEvasionLevel();

        if (
            stage < 1
                || stage > 4
                || data.isEmergencyEvasionOnCooldown(currentTick)
        ) {
            return false;
        }

        Vec3 lookDirection = player.getLookAngle();
        double horizontalLengthSquared =
            lookDirection.x * lookDirection.x
                + lookDirection.z * lookDirection.z;

        if (
            horizontalLengthSquared
                <= MIN_HORIZONTAL_DIRECTION_LENGTH_SQUARED
        ) {
            return false;
        }

        Vec3 evasionDirection = new Vec3(
            lookDirection.x,
            0.0D,
            lookDirection.z
        ).normalize();
        double horizontalSpeed =
            AdventurerConfig.getEmergencyEvasionHorizontalSpeed(stage);
        int cooldownTicks =
            AdventurerConfig.getEmergencyEvasionCooldownTicks(stage);
        double verticalSpeed = player.getDeltaMovement().y;

        player.setDeltaMovement(
            evasionDirection.x * horizontalSpeed,
            verticalSpeed,
            evasionDirection.z * horizontalSpeed
        );
        player.hasImpulse = true;
        player.hurtMarked = true;

        data.startEmergencyEvasionCooldown(
            currentTick,
            cooldownTicks
        );
        player.serverLevel().playSound(
            null,
            player.getX(),
            player.getY(),
            player.getZ(),
            SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS,
            0.6F,
            1.2F
        );

        return true;
    }

    private static boolean canEvadeFromCurrentMovementState(
        ServerPlayer player
    ) {
        return player.onGround()
            && !player.isPassenger()
            && !player.isSwimming()
            && !player.isFallFlying();
    }
}
