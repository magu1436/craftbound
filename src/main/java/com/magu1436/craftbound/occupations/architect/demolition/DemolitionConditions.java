package com.magu1436.craftbound.occupations.architect.demolition;

import com.magu1436.craftbound.registry.CraftboundAttributes;
import com.magu1436.craftbound.registry.CraftboundBlockTags;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * クライアントとサーバーで共通する解体作業の発動条件。
 */
public final class DemolitionConditions {

    private DemolitionConditions() {
    }

    public static boolean canApply(
        PlayerEvent.BreakSpeed event,
        BlockPos pos,
        boolean supportedGameMode
    ) {
        Player player = event.getEntity();
        BlockState state = event.getState();

        if (!supportedGameMode
            || player instanceof FakePlayer
            || player.isCreative()
            || player.isSpectator()
            || state.is(
                CraftboundBlockTags.ARCHITECT_DEMOLITION_BLACKLIST
            )
            || state.getDestroySpeed(player.level(), pos) < 0.0F) {
            return false;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()
            || tool.getDestroySpeed(state) <= 1.0F
            || !player.hasCorrectToolForDrops(state)) {
            return false;
        }

        return player.getAttributeValue(
            CraftboundAttributes.DEMOLITION_SPEED.get()
        ) > 1.0D;
    }

    public static void apply(PlayerEvent.BreakSpeed event) {
        double multiplier = event.getEntity().getAttributeValue(
            CraftboundAttributes.DEMOLITION_SPEED.get()
        );
        event.setNewSpeed(
            (float) (event.getNewSpeed() * multiplier)
        );
    }
}
