package com.magu1436.craftbound.occupations.adventurer.events;

import java.util.Objects;

import com.magu1436.craftbound.occupations.adventurer.AdventurerConfig;
import com.magu1436.craftbound.occupations.adventurer.capability.IAdventurerData;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineClearEffectDefinitions;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineExcludedDamageDefinitions;
import com.magu1436.craftbound.registry.CraftboundCapabilities;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 死線踏破の発動判定と発動後の保護を処理する。
 */
public final class DeathlineCrossingService {
    private static final long TICKS_PER_DAY = 24000L;
    private static final float SURVIVAL_HEALTH = 1.0F;
    private static final byte TOTEM_ACTIVATION_EVENT_ID = 35;

    private DeathlineCrossingService() {
    }

    /**
     * 致死ダメージに対する死線踏破の発動を試みる。
     *
     * @param player 対象プレイヤー
     * @param source 致死ダメージ
     * @return 発動した場合は {@code true}
     */
    public static boolean tryActivate(
        ServerPlayer player,
        DamageSource source
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(source, "damage source is null");

        if (isExcludedDamage(source)) {
            return false;
        }

        MinecraftServer server = Objects.requireNonNull(
            player.getServer(),
            "server is null"
        );
        ServerLevel overworld = server.overworld();
        long currentDay = Math.floorDiv(
            overworld.getDayTime(),
            TICKS_PER_DAY
        );
        long currentTick = overworld.getGameTime();

        return player
            .getCapability(CraftboundCapabilities.ADVENTURER_DATA)
            .map(
                data -> tryActivate(
                    player,
                    data,
                    currentDay,
                    currentTick
                )
            )
            .orElse(false);
    }

    /**
     * 死線踏破の保護によってダメージを無効化するかを返す。
     *
     * @param player 対象プレイヤー
     * @param source ダメージ
     * @return 無効化する場合は {@code true}
     */
    public static boolean shouldPreventDamage(
        ServerPlayer player,
        DamageSource source
    ) {
        Objects.requireNonNull(player, "player is null");
        Objects.requireNonNull(source, "damage source is null");

        if (isExcludedDamage(source)) {
            return false;
        }

        MinecraftServer server = Objects.requireNonNull(
            player.getServer(),
            "server is null"
        );
        long currentTick = server.overworld().getGameTime();

        return player
            .getCapability(CraftboundCapabilities.ADVENTURER_DATA)
            .map(
                data ->
                    data.isDeathlineCrossingProtected(currentTick)
            )
            .orElse(false);
    }

    private static boolean tryActivate(
        ServerPlayer player,
        IAdventurerData data,
        long currentDay,
        long currentTick
    ) {
        if (
            !data.hasDeathlineCrossing()
                || !data.canActivateDeathlineCrossing(currentDay)
        ) {
            return false;
        }

        data.recordDeathlineCrossingActivation(currentDay);
        data.startDeathlineCrossingProtection(
            currentTick,
            AdventurerConfig.getDeathlineCrossingProtectionTicks()
        );

        player.setHealth(SURVIVAL_HEALTH);

        if (AdventurerConfig.shouldDeathlineCrossingClearFire()) {
            player.clearFire();
        }
        if (AdventurerConfig.shouldDeathlineCrossingRestoreAir()) {
            player.setAirSupply(player.getMaxAirSupply());
        }

        clearConfiguredEffects(player);
        player.serverLevel().broadcastEntityEvent(
            player,
            TOTEM_ACTIVATION_EVENT_ID
        );

        return true;
    }

    private static void clearConfiguredEffects(ServerPlayer player) {
        var effectsToClear = player
            .getActiveEffects()
            .stream()
            .map(MobEffectInstance::getEffect)
            .filter(
                DeathlineClearEffectDefinitions.INSTANCE::shouldClear
            )
            .toList();

        effectsToClear.forEach(player::removeEffect);
    }

    private static boolean isExcludedDamage(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
            || DeathlineExcludedDamageDefinitions.INSTANCE.matches(
                source
            );
    }
}
