package com.magu1436.craftbound.occupations.blacksmith.carving.menu;

import com.magu1436.craftbound.occupations.blacksmith.carving.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.session.CarvingSessionState;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.registry.CraftboundMenus;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class CarvingMenu extends AbstractContainerMenu {
    public static final int COMPLETE_BUTTON = 0;
    private final Level level; private final BlockPos pos;
    public CarvingMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, data.readBlockPos()); }
    public CarvingMenu(int id, Inventory inventory, BlockPos pos) {
        super(CraftboundMenus.CARVING.get(), id); this.level = inventory.player.level(); this.pos = pos.immutable();
    }
    public Optional<CarvingTableBlockEntity> getCarvingTable() {
        return level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table ? Optional.of(table) : Optional.empty();
    }
    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != COMPLETE_BUTTON || !(player instanceof ServerPlayer serverPlayer)) return false;
        CarvingTableBlockEntity table = getCarvingTable().orElse(null);
        if (table == null || table.activeSession() == null
            || !table.activeSession().activePlayerId().equals(player.getUUID())) return false;
        boolean completed = CarvingGameService.finalizeCarving(table, serverPlayer)
            == CarvingGameService.FinalizeResult.COMPLETED;
        if (completed) { BlacksmithOperationSessionRegistry.release(table); player.closeContainer(); }
        return completed;
    }
    @Override public boolean stillValid(Player player) { return CarvingPartSelectionMenu.valid(level, pos, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!level.isClientSide()) getCarvingTable().ifPresent(BlacksmithOperationSessionRegistry::release);
    }
    public static void open(ServerPlayer player, CarvingTableBlockEntity table, CarvingSessionState session) {
        BlockPos pos = table.getBlockPos();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new CarvingMenu(containerId, inventory, pos),
            Component.translatable("container.craftbound.carving_table")), buffer -> buffer.writeBlockPos(pos));
        CarvingPacketHandler.syncFull(player, table, session);
    }
}
