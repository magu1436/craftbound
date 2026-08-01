package com.magu1436.craftbound.occupations.architect;

import java.util.Objects;
import java.util.UUID;

import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 足場移動による水平・垂直移動速度の補正を管理する。
 */
public final class ScaffoldingMobilityService {

    private static final UUID HORIZONTAL_SPEED_MODIFIER_ID =
        UUID.fromString("3ad92f3d-ed70-45a4-a909-8e25f2175218");
    private static final String HORIZONTAL_SPEED_MODIFIER_NAME =
        "scaffolding_mobility";

    private ScaffoldingMobilityService() {
    }

    public static void updateHorizontalSpeed(ServerPlayer player) {
        AttributeInstance movementSpeed = Objects.requireNonNull(
            player.getAttribute(Attributes.MOVEMENT_SPEED),
            "Movement speed attribute is null"
        );
        double bonus = canApply(player) && isOnScaffolding(player)
            ? getAttributeBonus(player)
            : 0.0D;
        AttributeModifier current = movementSpeed.getModifier(
            HORIZONTAL_SPEED_MODIFIER_ID
        );

        if (bonus <= 0.0D) {
            if (current != null) {
                movementSpeed.removeModifier(HORIZONTAL_SPEED_MODIFIER_ID);
            }
            return;
        }

        if (current != null
            && Double.compare(current.getAmount(), bonus) == 0) {
            return;
        }

        movementSpeed.removeModifier(HORIZONTAL_SPEED_MODIFIER_ID);
        movementSpeed.addTransientModifier(
            new AttributeModifier(
                HORIZONTAL_SPEED_MODIFIER_ID,
                HORIZONTAL_SPEED_MODIFIER_NAME,
                bonus,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            )
        );
    }

    public static double getVerticalSpeedBonus(Player player) {
        if (!canApply(player) || !isInsideScaffolding(player)) {
            return 0.0D;
        }

        return getAttributeBonus(player);
    }

    public static boolean isInsideScaffolding(Player player) {
        return player.level().getBlockState(player.blockPosition())
            .is(Blocks.SCAFFOLDING);
    }

    private static boolean isOnScaffolding(Player player) {
        BlockPos feet = player.blockPosition();
        return player.level().getBlockState(feet).is(Blocks.SCAFFOLDING)
            || player.level().getBlockState(feet.below())
                .is(Blocks.SCAFFOLDING);
    }

    private static double getAttributeBonus(Player player) {
        return Mth.clamp(
            player.getAttributeValue(
                CraftboundAttributes.SCAFFOLDING_MOBILITY.get()
            ),
            0.0D,
            0.5D
        );
    }

    private static boolean canApply(Player player) {
        if (player instanceof FakePlayer
            || player instanceof ServerPlayer serverPlayer
                && !isSupportedGameMode(
                    serverPlayer.gameMode.getGameModeForPlayer()
                )
            || player.isCreative()
            || player.isSpectator()
            || player.isPassenger()
            || player.isFallFlying()
            || player.isSwimming()
            || player.getAbilities().flying) {
            return false;
        }

        return true;
    }

    private static boolean isSupportedGameMode(GameType gameType) {
        return gameType == GameType.SURVIVAL
            || gameType == GameType.ADVENTURE;
    }
}
