package com.magu1436.craftbound.occupations.blacksmith.carving.menu;

import com.magu1436.craftbound.occupations.blacksmith.carving.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinition;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.registry.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

public final class CarvingPartSelectionMenu extends AbstractContainerMenu {
    private final Level level; private final BlockPos pos; private final List<Candidate> candidates;
    public CarvingPartSelectionMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos(), readIds(data));
    }
    public CarvingPartSelectionMenu(int id, Inventory inventory, BlockPos pos, List<Candidate> candidates) {
        super(CraftboundMenus.CARVING_PART_SELECTION.get(), id);
        this.level = inventory.player.level(); this.pos = pos.immutable(); this.candidates = List.copyOf(candidates);
    }
    public List<Candidate> candidates() { return candidates; }
    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer) || buttonId < 0 || buttonId >= candidates.size()) return false;
        CarvingTableBlockEntity table = table().orElse(null);
        if (table == null || CarvingGameService.start(table, serverPlayer, candidates.get(buttonId).id()).isEmpty()) return false;
        var session = BlacksmithOperationSessionRegistry.acquire(serverPlayer, table);
        if (session.isEmpty()) return false;
        CarvingMenu.open(serverPlayer, table, session.get());
        return true;
    }
    @Override public boolean stillValid(Player player) { return valid(level, pos, player); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    private Optional<CarvingTableBlockEntity> table() {
        return level.getBlockEntity(pos) instanceof CarvingTableBlockEntity table ? Optional.of(table) : Optional.empty();
    }
    static boolean valid(Level level, BlockPos pos, Player player) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).is(CraftboundBlocks.CARVING_TABLE.get())
            && player.distanceToSqr(pos.getCenter()) <= 64.0D;
    }
    public static void open(ServerPlayer player, BlockPos pos, List<NonMetalPartDefinition> definitions) {
        List<Candidate> ids = definitions.stream().map(value -> new Candidate(value.id(),
            new ItemStack(ForgeRegistries.ITEMS.getValue(value.outputItemId())))).toList();
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (containerId, inventory, ignored) -> new CarvingPartSelectionMenu(containerId, inventory, pos, ids),
            Component.translatable("container.craftbound.carving_part_selection")), buffer -> write(buffer, pos, ids));
    }
    private static void write(FriendlyByteBuf buffer, BlockPos pos, List<Candidate> ids) {
        buffer.writeBlockPos(pos); buffer.writeVarInt(ids.size());
        ids.forEach(value -> { buffer.writeResourceLocation(value.id()); buffer.writeItem(value.output()); });
    }
    private static List<Candidate> readIds(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 256); List<Candidate> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(new Candidate(buffer.readResourceLocation(), buffer.readItem())); return ids;
    }
    public record Candidate(ResourceLocation id, ItemStack output) {}
}
