package com.magu1436.craftbound.occupations.blacksmith.forging.menu;

import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettingsDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.registry.CraftboundBlocks;
import com.magu1436.craftbound.registry.CraftboundMenus;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class ForgingMenu extends AbstractContainerMenu {
    private final Level level;
    private final BlockPos blockPos;

    public ForgingMenu(int containerId, Inventory inventory, FriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos());
    }

    public ForgingMenu(int containerId, Inventory inventory, BlockPos blockPos) {
        super(CraftboundMenus.FORGING_TABLE.get(), containerId);
        this.level = inventory.player.level();
        this.blockPos = blockPos.immutable();
    }

    public BlockPos getBlockPos() { return blockPos; }

    public Optional<ForgingTableBlockEntity> getForgingTable() {
        if (!level.hasChunkAt(blockPos)) return Optional.empty();
        return level.getBlockEntity(blockPos) instanceof ForgingTableBlockEntity table
            ? Optional.of(table) : Optional.empty();
    }

    @Override
    public boolean stillValid(Player player) {
        if (!level.hasChunkAt(blockPos)
            || !level.getBlockState(blockPos).is(CraftboundBlocks.FORGING_TABLE.get())) return false;
        if (level.isClientSide()) return true;
        return BlacksmithSettingsDefinitions.INSTANCE.get()
            .map(settings -> player.distanceToSqr(
                blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D
            ) <= Math.pow(settings.network().workbenchInteractionDistance(), 2.0D))
            .orElse(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            getForgingTable().ifPresent(table ->
                BlacksmithOperationSessionRegistry.release(table, true));
        }
    }
}
