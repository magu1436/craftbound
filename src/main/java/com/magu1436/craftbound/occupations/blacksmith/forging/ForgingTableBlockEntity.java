package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressStateCodec;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.ForgingSessionState;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ForgingTableBlockEntity extends BlockEntity {
    private static final String TAG_WORKING_PART = "WorkingPart";
    private static final String TAG_PENDING_OUTPUTS = "PendingOutputs";
    private static final String TAG_DISPLAY_STACK = "DisplayStack";
    private static final String TAG_HAS_DISPLAY_STACK = "HasDisplayStack";

    private ItemStack workingPart = ItemStack.EMPTY;
    private final List<ItemStack> pendingOutputs = new ArrayList<>();
    private boolean completionReserved;
    private boolean invalidStoredState;
    private ForgingSessionState activeSession;
    private ItemStack clientDisplayStack = ItemStack.EMPTY;

    public ForgingTableBlockEntity(BlockPos pos, BlockState state) {
        super(CraftboundBlockEntities.FORGING_TABLE.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ItemStack savedWorkingPart = workingPart.copy();
        if (!savedWorkingPart.isEmpty() && activeSession != null && level != null) {
            try {
                ForgingGaugeCalculator.GaugeSnapshot gauge =
                    BlacksmithOperationSessionRegistry.currentGauge(activeSession, level.getGameTime());
                ForgingProgressStateService.saveGauge(savedWorkingPart, gauge.value(), gauge.direction());
            } catch (RuntimeException ignored) {
                // Preserve the last valid snapshot when reload data is unavailable.
            }
        }
        if (!savedWorkingPart.isEmpty()) {
            tag.put(TAG_WORKING_PART, savedWorkingPart.save(new CompoundTag()));
        }

        ListTag outputTags = new ListTag();
        for (ItemStack output : pendingOutputs) {
            outputTags.add(output.save(new CompoundTag()));
        }
        tag.put(TAG_PENDING_OUTPUTS, outputTags);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        workingPart = ItemStack.EMPTY;
        pendingOutputs.clear();
        completionReserved = false;
        activeSession = null;
        invalidStoredState = false;

        if (tag.contains(TAG_WORKING_PART)) {
            if (!tag.contains(TAG_WORKING_PART, Tag.TAG_COMPOUND)) {
                invalidStoredState = true;
            } else {
                workingPart = ItemStack.of(tag.getCompound(TAG_WORKING_PART));
                invalidStoredState = workingPart.isEmpty()
                    || !ForgingProgressStateService.isUsable(workingPart);
            }
        }

        if (tag.contains(TAG_PENDING_OUTPUTS)) {
            if (!tag.contains(TAG_PENDING_OUTPUTS, Tag.TAG_LIST)) {
                invalidStoredState = true;
            } else {
                ListTag outputTags = (ListTag) tag.get(TAG_PENDING_OUTPUTS);
                if (!outputTags.isEmpty() && outputTags.getElementType() != Tag.TAG_COMPOUND) {
                    invalidStoredState = true;
                } else {
                    for (int index = 0; index < outputTags.size(); index++) {
                        ItemStack output = ItemStack.of(outputTags.getCompound(index));
                        if (output.isEmpty()) {
                            invalidStoredState = true;
                        } else {
                            pendingOutputs.add(output);
                        }
                    }
                }
            }
        }
    }

    public boolean hasWorkingPart() {
        return !workingPart.isEmpty();
    }

    public ItemStack getWorkingPart() {
        return workingPart.copy();
    }

    public boolean hasPendingOutputs() {
        return !pendingOutputs.isEmpty();
    }

    public ForgingSessionState getActiveSession() {
        return activeSession;
    }

    public boolean setActiveSession(ForgingSessionState session) {
        if (session == null || invalidStoredState || !hasWorkingPart() || hasPendingOutputs()) return false;
        if (activeSession != null && !activeSession.activePlayerId().equals(session.activePlayerId())) return false;
        activeSession = session;
        return true;
    }

    public void clearActiveSession(UUID sessionId) {
        if (activeSession != null && activeSession.sessionId().equals(sessionId)) activeSession = null;
    }

    public List<ItemStack> getPendingOutputs() {
        return pendingOutputs.stream().map(ItemStack::copy).toList();
    }

    public ItemStack getDisplayStack() {
        if (level != null && level.isClientSide()) return clientDisplayStack.copy();
        if (!workingPart.isEmpty()) return workingPart.copy();
        return pendingOutputs.isEmpty() ? ItemStack.EMPTY : pendingOutputs.get(0).copy();
    }

    public boolean tryInsertWorkingPart(ServerPlayer player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND
            || hasWorkingPart()
            || hasPendingOutputs()
            || invalidStoredState) {
            return false;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.getCount() != 1
            || !ForgingProgressStateService.isUsable(heldItem)) {
            return false;
        }

        workingPart = heldItem;
        player.setItemInHand(hand, ItemStack.EMPTY);
        markChangedAndSync();
        return true;
    }

    public boolean tryExtractWorkingPart(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()
            || !hasWorkingPart()
            || hasPendingOutputs()
            || invalidStoredState) {
            return false;
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, workingPart);
        workingPart = ItemStack.EMPTY;
        markChangedAndSync();
        return true;
    }

    public boolean tryCollectPendingOutputs(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()
            || !hasPendingOutputs()
            || invalidStoredState) {
            return false;
        }

        Inventory inventory = player.getInventory();
        List<ItemStack> simulatedItems = inventory.items.stream()
            .map(ItemStack::copy)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        if (!storeAll(simulatedItems, pendingOutputs, inventory.getMaxStackSize())) {
            return false;
        }

        for (int index = 0; index < inventory.items.size(); index++) {
            inventory.items.set(index, simulatedItems.get(index));
        }
        inventory.setChanged();
        pendingOutputs.clear();
        markChangedAndSync();
        return true;
    }

    public boolean addPendingOutput(ItemStack output) {
        if (output.isEmpty() || invalidStoredState) return false;
        pendingOutputs.add(output.copy());
        markChangedAndSync();
        return true;
    }

    public boolean updateWorkingPart(ItemStack updatedWorkingPart) {
        if (updatedWorkingPart.isEmpty()
            || !hasWorkingPart()
            || completionReserved
            || invalidStoredState
            || RoughMetalPartStateService.read(updatedWorkingPart).isEmpty()) {
            return false;
        }
        workingPart = updatedWorkingPart.copy();
        markChangedAndSync();
        return true;
    }

    public boolean tryReserveCompletion() {
        if (completionReserved
            || invalidStoredState
            || !hasWorkingPart()
            || hasPendingOutputs()) {
            return false;
        }
        completionReserved = true;
        return true;
    }

    public void cancelCompletionReservation() {
        completionReserved = false;
    }

    public boolean commitReservedResult(ServerPlayer player, ItemStack output) {
        if (!completionReserved
            || invalidStoredState
            || !hasWorkingPart()
            || hasPendingOutputs()
            || output.isEmpty()) {
            return false;
        }

        Inventory inventory = player.getInventory();
        List<ItemStack> simulatedItems = inventory.items.stream()
            .map(ItemStack::copy)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        if (storeAll(simulatedItems, List.of(output), inventory.getMaxStackSize())) {
            for (int index = 0; index < inventory.items.size(); index++) {
                inventory.items.set(index, simulatedItems.get(index));
            }
            inventory.setChanged();
        } else {
            pendingOutputs.add(output.copy());
        }

        workingPart = ItemStack.EMPTY;
        completionReserved = false;
        markChangedAndSync();
        return true;
    }

    public void dropContents(ServerLevel level) {
        if (!workingPart.isEmpty()) {
            drop(level, workingPart);
        }
        for (ItemStack output : pendingOutputs) {
            drop(level, output);
        }
        workingPart = ItemStack.EMPTY;
        pendingOutputs.clear();
        completionReserved = false;
        invalidStoredState = false;
        setChanged();
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        ItemStack displayStack = getDisplayStack();
        tag.putBoolean(TAG_HAS_DISPLAY_STACK, !displayStack.isEmpty());
        if (!displayStack.isEmpty()) {
            CompoundTag stackTag = displayStack.getTag();
            if (stackTag != null) stackTag.remove(ForgingProgressStateCodec.ROOT_KEY);
            tag.put(TAG_DISPLAY_STACK, displayStack.save(new CompoundTag()));
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
        Connection connection,
        ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();
        if (tag == null) {
            clientDisplayStack = ItemStack.EMPTY;
            return;
        }
        handleUpdateTag(tag);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        boolean hasDisplayStack = tag.contains(TAG_HAS_DISPLAY_STACK, Tag.TAG_BYTE)
            ? tag.getBoolean(TAG_HAS_DISPLAY_STACK)
            : tag.contains(TAG_DISPLAY_STACK, Tag.TAG_COMPOUND);
        clientDisplayStack = hasDisplayStack && tag.contains(TAG_DISPLAY_STACK, Tag.TAG_COMPOUND)
            ? ItemStack.of(tag.getCompound(TAG_DISPLAY_STACK))
            : ItemStack.EMPTY;
    }

    private void markChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void drop(ServerLevel level, ItemStack stack) {
        Containers.dropItemStack(
            level,
            worldPosition.getX() + 0.5D,
            worldPosition.getY() + 0.75D,
            worldPosition.getZ() + 0.5D,
            stack
        );
    }

    private static boolean storeAll(
        List<ItemStack> inventoryItems,
        List<ItemStack> outputs,
        int inventoryStackLimit
    ) {
        for (ItemStack output : outputs) {
            int remaining = output.getCount();
            for (ItemStack inventoryItem : inventoryItems) {
                if (!inventoryItem.isEmpty()
                    && ItemStack.isSameItemSameTags(inventoryItem, output)) {
                    int limit = Math.min(inventoryStackLimit, inventoryItem.getMaxStackSize());
                    int moved = Math.min(remaining, limit - inventoryItem.getCount());
                    if (moved > 0) {
                        inventoryItem.grow(moved);
                        remaining -= moved;
                    }
                }
            }
            for (int index = 0; index < inventoryItems.size() && remaining > 0; index++) {
                if (inventoryItems.get(index).isEmpty()) {
                    ItemStack inserted = output.copy();
                    int moved = Math.min(remaining, Math.min(inventoryStackLimit, inserted.getMaxStackSize()));
                    inserted.setCount(moved);
                    inventoryItems.set(index, inserted);
                    remaining -= moved;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
}
