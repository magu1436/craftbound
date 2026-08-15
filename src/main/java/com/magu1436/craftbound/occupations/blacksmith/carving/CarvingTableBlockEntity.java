package com.magu1436.craftbound.occupations.blacksmith.carving;

import com.magu1436.craftbound.occupations.blacksmith.carving.state.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.session.CarvingSessionState;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

public final class CarvingTableBlockEntity extends BlockEntity {
    private ItemStack material = ItemStack.EMPTY;
    private CarvingProgressState progress;
    private final List<ItemStack> pendingOutputs = new ArrayList<>();
    private boolean completionReserved;
    private CarvingSessionState activeSession;
    private ItemStack clientMaterial = ItemStack.EMPTY;

    public CarvingTableBlockEntity(BlockPos pos, BlockState state) {
        super(CraftboundBlockEntities.CARVING_TABLE.get(), pos, state);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!material.isEmpty()) tag.put("Material", material.save(new CompoundTag()));
        if (progress != null) tag.put("Progress", CarvingProgressStateCodec.write(progress));
        ListTag outputs = new ListTag(); for (ItemStack output : pendingOutputs) outputs.add(output.save(new CompoundTag()));
        tag.put("PendingOutputs", outputs);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); completionReserved = false; activeSession = null; pendingOutputs.clear(); progress = null;
        material = tag.contains("Material", Tag.TAG_COMPOUND) ? ItemStack.of(tag.getCompound("Material")) : ItemStack.EMPTY;
        if (tag.contains("Progress", Tag.TAG_COMPOUND)) progress = CarvingProgressStateCodec.read(tag.getCompound("Progress")).orElse(null);
        if (tag.contains("PendingOutputs", Tag.TAG_LIST)) {
            ListTag values = tag.getList("PendingOutputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < values.size(); i++) { ItemStack stack = ItemStack.of(values.getCompound(i)); if (!stack.isEmpty()) pendingOutputs.add(stack); }
        }
        if (material.isEmpty()) progress = null;
    }
    public ItemStack material() { return material.copy(); }
    public ItemStack displayMaterial() { return level != null && level.isClientSide() ? clientMaterial.copy() : material.copy(); }
    public Optional<CarvingProgressState> progress() { return Optional.ofNullable(progress); }
    public CarvingSessionState activeSession() { return activeSession; }
    public synchronized boolean setActiveSession(CarvingSessionState session) {
        if (progress == null || hasPendingOutputs()) return false;
        if (activeSession != null && !activeSession.activePlayerId().equals(session.activePlayerId())) return false;
        activeSession = session; return true;
    }
    public synchronized void clearActiveSession(UUID sessionId) {
        if (activeSession != null && activeSession.sessionId().equals(sessionId)) activeSession = null;
    }
    public boolean hasPendingOutputs() { return !pendingOutputs.isEmpty(); }
    public synchronized boolean insertMaterial(ItemStack stack) {
        if (!material.isEmpty() || stack.isEmpty() || hasPendingOutputs()) return false;
        material = stack.copy(); material.setCount(1); changed(); return true;
    }
    public synchronized ItemStack takeUnselectedMaterial() {
        if (material.isEmpty() || progress != null || hasPendingOutputs()) return ItemStack.EMPTY;
        ItemStack result = material; material = ItemStack.EMPTY; changed(); return result;
    }
    public synchronized boolean start(CarvingProgressState state) {
        if (material.isEmpty() || progress != null || hasPendingOutputs()) return false;
        progress = state; changed(); return true;
    }
    public synchronized boolean updateProgress(CarvingProgressState state) {
        if (progress == null || !progress.processId().equals(state.processId()) || completionReserved) return false;
        progress = state; changed(); return true;
    }
    public synchronized boolean reserveCompletion() {
        if (completionReserved || material.isEmpty() || hasPendingOutputs()) return false;
        completionReserved = true; return true;
    }
    public synchronized void cancelCompletion() { completionReserved = false; }
    public synchronized boolean commitOutput(ItemStack output) {
        if (!completionReserved || output.isEmpty()) return false;
        pendingOutputs.add(output.copy()); material = ItemStack.EMPTY; progress = null; activeSession = null;
        completionReserved = false; changed(); return true;
    }
    public synchronized boolean clearBroken() {
        if (material.isEmpty() || progress == null) return false;
        material = ItemStack.EMPTY; progress = null; activeSession = null; completionReserved = false; changed(); return true;
    }
    public synchronized boolean collect(ServerPlayer player) {
        if (pendingOutputs.isEmpty()) return false;
        for (ItemStack output : pendingOutputs) {
            ItemStack remaining = output.copy(); player.getInventory().add(remaining);
            if (!remaining.isEmpty() && level instanceof ServerLevel server) drop(server, remaining);
        }
        pendingOutputs.clear(); changed(); return true;
    }
    public synchronized void dropContents(ServerLevel level) {
        if (!material.isEmpty()) drop(level, material);
        for (ItemStack output : pendingOutputs) drop(level, output);
        material = ItemStack.EMPTY; progress = null; pendingOutputs.clear(); completionReserved = false; setChanged();
    }
    private void drop(ServerLevel level, ItemStack stack) {
        Containers.dropItemStack(level, worldPosition.getX() + .5D, worldPosition.getY() + .75D,
            worldPosition.getZ() + .5D, stack);
    }
    private void changed() {
        setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (!material.isEmpty()) tag.put("Material", material.save(new CompoundTag()));
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) handleUpdateTag(packet.getTag());
    }
    @Override public void handleUpdateTag(CompoundTag tag) {
        clientMaterial = tag.contains("Material", Tag.TAG_COMPOUND)
            ? ItemStack.of(tag.getCompound("Material")) : ItemStack.EMPTY;
    }
}
