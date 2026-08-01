package com.magu1436.craftbound.occupations.explorer;

import com.magu1436.craftbound.occupations.explorer.data.ToolCareBlockDefinitions;
import com.magu1436.craftbound.registry.CraftboundAttributes;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;

/**
 * 道具の手入れによるブロック破壊時の耐久消費軽減を適用する。
 */
public final class ToolCareDurabilityService {

    private static final ThreadLocal<BlockBreakContext> BLOCK_BREAK_CONTEXT =
        new ThreadLocal<>();

    private ToolCareDurabilityService() {
    }

    /**
     * ブロック破壊による耐久消費処理の開始を記録する。
     */
    public static void beginBlockBreak(
        Player player,
        BlockState state
    ) {
        BLOCK_BREAK_CONTEXT.set(new BlockBreakContext(player, state));
    }

    /**
     * ブロック破壊による耐久消費処理の記録を破棄する。
     */
    public static void endBlockBreak() {
        BLOCK_BREAK_CONTEXT.remove();
    }

    /**
     * 道具の手入れが発動した場合に耐久消費を防ぐ。
     */
    public static boolean shouldPreventDurabilityConsumption(
        ServerPlayer player
    ) {
        if (!isEligible(player)) {
            return false;
        }

        double chance = player.getAttributeValue(
            CraftboundAttributes.TOOL_CARE.get()
        );
        return chance > 0.0D
            && player.getRandom().nextDouble() < chance;
    }

    private static boolean isEligible(ServerPlayer player) {
        BlockBreakContext context = BLOCK_BREAK_CONTEXT.get();
        return context != null
            && context.player() == player
            && ToolCareBlockDefinitions.INSTANCE.contains(
                context.state().getBlock()
            )
            && !(player instanceof FakePlayer)
            && player.gameMode.getGameModeForPlayer()
                == GameType.SURVIVAL;
    }

    private record BlockBreakContext(
        Player player,
        BlockState state
    ) {
    }
}
