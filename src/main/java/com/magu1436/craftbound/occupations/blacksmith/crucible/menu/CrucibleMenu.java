package com.magu1436.craftbound.occupations.blacksmith.crucible.menu;

import java.util.Optional;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleInsertResult;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleProcessState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleStateService;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.registry.CraftboundMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CrucibleMenu extends AbstractContainerMenu {
    public static final int DISCARD_BUTTON_ID = 0;

    private static final int MIN_HOTBAR_SLOT = 0;
    private static final int MAX_HOTBAR_SLOT = 8;
    private static final int INPUT_SLOT_INDEX = 0;
    private static final int PLAYER_SLOT_START = 1;
    private static final int PLAYER_SLOT_END = 37;
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;

    private final Inventory playerInventory;
    private final Container inputContainer = new SimpleContainer(1);
    private final int targetHotbarSlot;
    private final ItemStack initialCrucibleReference;
    private final boolean clientSide;
    private boolean processingInput;
    private boolean valid = true;

    public CrucibleMenu(
        int containerId,
        Inventory playerInventory,
        FriendlyByteBuf additionalData
    ) {
        this(containerId, playerInventory, additionalData.readVarInt());
    }

    public CrucibleMenu(
        int containerId,
        Inventory playerInventory,
        int targetHotbarSlot
    ) {
        super(CraftboundMenus.CRUCIBLE.get(), containerId);
        this.playerInventory = playerInventory;
        this.targetHotbarSlot = targetHotbarSlot;
        this.clientSide = playerInventory.player.level().isClientSide();
        this.initialCrucibleReference = isHotbarSlot(targetHotbarSlot)
            ? playerInventory.getItem(targetHotbarSlot)
            : ItemStack.EMPTY;

        addSlot(new CrucibleInputSlot(
            inputContainer,
            0,
            27,
            36,
            MetalMaterialDefinitions.INSTANCE,
            clientSide,
            this::handleInputChanged
        ));
        addPlayerInventorySlots();
    }

    public int getTargetHotbarSlot() {
        return targetHotbarSlot;
    }

    public Optional<CrucibleState> getCrucibleState() {
        if (!isTargetValid(playerInventory.player)) {
            return Optional.empty();
        }
        return CrucibleStateService.read(currentCrucible());
    }

    @Override
    public boolean stillValid(Player player) {
        valid = valid && isTargetValid(player);
        return valid;
    }

    @Override
    public void clicked(
        int slotId,
        int button,
        ClickType clickType,
        Player player
    ) {
        if (isTargetMenuSlot(slotId)
            || (clickType == ClickType.SWAP && button == targetHotbarSlot)
            || clickType == ClickType.PICKUP_ALL) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return !isTargetPlayerSlot(slot) && super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return !isTargetPlayerSlot(slot)
            && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!valid || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem() || isTargetMenuSlot(index)) {
            return ItemStack.EMPTY;
        }
        ItemStack source = sourceSlot.getItem();
        ItemStack original = source.copy();

        if (index == INPUT_SLOT_INDEX) {
            if (!moveItemStackTo(
                source,
                PLAYER_SLOT_START,
                PLAYER_SLOT_END,
                true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (player.level().isClientSide() || !isTargetValid(player)) {
                return ItemStack.EMPTY;
            }
            CrucibleInsertResult result = CrucibleStateService.insert(
                currentCrucible(),
                source,
                source.getCount(),
                MetalMaterialDefinitions.INSTANCE
            );
            if (!result.succeeded()) {
                return ItemStack.EMPTY;
            }
            source.shrink(result.acceptedItemCount());
            syncCrucibleChange();
        }

        if (source.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != DISCARD_BUTTON_ID
            || player.level().isClientSide()
            || !stillValid(player)) {
            return false;
        }

        boolean discarded = CrucibleStateService.discardAll(
            currentCrucible()
        );
        if (discarded) {
            syncCrucibleChange();
        }
        return discarded;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            clearContainer(player, inputContainer);
        }
    }

    private void handleInputChanged(CrucibleInputSlot inputSlot) {
        if (processingInput
            || playerInventory.player.level().isClientSide()
            || !inputSlot.hasItem()) {
            return;
        }

        processingInput = true;
        try {
            ItemStack input = inputSlot.getItem();
            if (!isTargetValid(playerInventory.player)) {
                valid = false;
                returnInput(inputSlot);
                return;
            }

            CrucibleInsertResult result = CrucibleStateService.insert(
                currentCrucible(),
                input,
                1,
                MetalMaterialDefinitions.INSTANCE
            );
            if (result.succeeded()) {
                input.shrink(result.acceptedItemCount());
                inputSlot.set(input.isEmpty() ? ItemStack.EMPTY : input);
                syncCrucibleChange();
            } else {
                returnInput(inputSlot);
            }
        } finally {
            processingInput = false;
        }
    }

    private void returnInput(CrucibleInputSlot inputSlot) {
        ItemStack rejected = inputSlot.getItem().copy();
        inputSlot.set(ItemStack.EMPTY);
        playerInventory.placeItemBackInInventory(rejected);
    }

    private void syncCrucibleChange() {
        ItemStack crucible = currentCrucible();
        playerInventory.setItem(targetHotbarSlot, crucible);
        playerInventory.setChanged();
        broadcastChanges();
    }

    private boolean isTargetValid(Player player) {
        if (!valid || !player.isAlive() || !isHotbarSlot(targetHotbarSlot)) {
            return false;
        }
        ItemStack current = currentCrucible();
        return (clientSide || current == initialCrucibleReference)
            && current.is(CraftboundItems.CRUCIBLE.get())
            && CrucibleStateService.read(current).isPresent();
    }

    private ItemStack currentCrucible() {
        return isHotbarSlot(targetHotbarSlot)
            ? playerInventory.getItem(targetHotbarSlot)
            : ItemStack.EMPTY;
    }

    private boolean isTargetMenuSlot(int menuSlotIndex) {
        if (menuSlotIndex < PLAYER_SLOT_START
            || menuSlotIndex >= PLAYER_SLOT_END) {
            return false;
        }
        Slot slot = slots.get(menuSlotIndex);
        return isTargetPlayerSlot(slot);
    }

    private boolean isTargetPlayerSlot(Slot slot) {
        return slot.container == playerInventory
            && slot.getContainerSlot() == targetHotbarSlot;
    }

    private void addPlayerInventorySlots() {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                    playerInventory,
                    column + row * 9 + 9,
                    PLAYER_INVENTORY_X + column * 18,
                    PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                playerInventory,
                column,
                PLAYER_INVENTORY_X + column * 18,
                HOTBAR_Y
            ));
        }
    }

    private static boolean isHotbarSlot(int slot) {
        return slot >= MIN_HOTBAR_SLOT && slot <= MAX_HOTBAR_SLOT;
    }
}
