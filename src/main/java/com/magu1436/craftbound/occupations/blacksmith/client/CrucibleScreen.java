package com.magu1436.craftbound.occupations.blacksmith.client;

import java.util.Optional;
import java.util.Locale;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.menu.CrucibleMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CrucibleScreen
    extends AbstractContainerScreen<CrucibleMenu> {

    private static final int DISCARD_CONFIRM_TICKS = 100;
    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_SLOT_X = 26;
    private static final int INPUT_SLOT_Y = 35;
    private static final int PLAYER_SLOTS_X = 7;
    private static final int PLAYER_SLOTS_Y = 83;

    private Button discardButton;
    private int discardConfirmationTicks;

    public CrucibleScreen(
        CrucibleMenu menu,
        Inventory playerInventory,
        Component title
    ) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        discardButton = Button.builder(
            Component.translatable("screen.craftbound.crucible.discard"),
            button -> handleDiscardClick()
        ).bounds(leftPos + 112, topPos + 33, 52, 20).build();
        addRenderableWidget(discardButton);
        updateDiscardButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (discardConfirmationTicks > 0) {
            discardConfirmationTicks--;
            if (discardConfirmationTicks == 0) {
                updateDiscardButton();
            }
        }
        updateDiscardButtonAvailability();
    }

    @Override
    public void render(
        GuiGraphics graphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (discardConfirmationTicks > 0) {
            graphics.drawCenteredString(
                font,
                Component.translatable(
                    "screen.craftbound.crucible.discard_warning"
                ),
                width / 2,
                topPos + 62,
                0xA00000
            );
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(
        GuiGraphics graphics,
        float partialTick,
        int mouseX,
        int mouseY
    ) {
        graphics.fill(
            leftPos,
            topPos,
            leftPos + imageWidth,
            topPos + imageHeight,
            BACKGROUND_COLOR
        );
        drawFrame(graphics, 0, 0, imageWidth, imageHeight);
        drawSlot(graphics, INPUT_SLOT_X, INPUT_SLOT_Y);
        drawPlayerInventorySlots(graphics);
    }

    @Override
    protected void renderLabels(
        GuiGraphics graphics,
        int mouseX,
        int mouseY
    ) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(
            font,
            playerInventoryTitle,
            inventoryLabelX,
            inventoryLabelY,
            0x404040,
            false
        );

        Optional<CrucibleState> stateResult = menu.getCrucibleState();
        if (stateResult.isEmpty()) {
            graphics.drawString(
                font,
                Component.translatable(
                    "screen.craftbound.crucible.invalid_state"
                ),
                52,
                34,
                0xA00000,
                false
            );
            return;
        }

        CrucibleState state = stateResult.get();
        graphics.drawString(
            font,
            Component.translatable(
                "screen.craftbound.crucible.metal",
                metalName(state.metalId())
            ),
            52,
            27,
            0x404040,
            false
        );
        graphics.drawString(
            font,
            Component.translatable(
                "screen.craftbound.crucible.amount",
                state.amount(),
                CrucibleState.MAX_CAPACITY
            ),
            52,
            40,
            0x404040,
            false
        );
        graphics.drawString(
            font,
            Component.translatable(
                "screen.craftbound.crucible.state",
                Component.translatable(
                        "screen.craftbound.crucible.state."
                        + state.processState().name().toLowerCase(Locale.ROOT)
                )
            ),
            52,
            53,
            0x404040,
            false
        );
    }

    private void handleDiscardClick() {
        if (!hasContents()) {
            clearDiscardConfirmation();
            return;
        }
        if (discardConfirmationTicks == 0) {
            discardConfirmationTicks = DISCARD_CONFIRM_TICKS;
            updateDiscardButton();
            return;
        }

        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                CrucibleMenu.DISCARD_BUTTON_ID
            );
        }
        clearDiscardConfirmation();
    }

    private void updateDiscardButtonAvailability() {
        if (discardButton == null) {
            return;
        }
        discardButton.active = hasContents();
        if (!discardButton.active && discardConfirmationTicks > 0) {
            clearDiscardConfirmation();
        }
    }

    private void updateDiscardButton() {
        if (discardButton == null) {
            return;
        }
        discardButton.setMessage(Component.translatable(
            discardConfirmationTicks > 0
                ? "screen.craftbound.crucible.discard_confirm"
                : "screen.craftbound.crucible.discard"
        ));
    }

    private void clearDiscardConfirmation() {
        discardConfirmationTicks = 0;
        updateDiscardButton();
    }

    private boolean hasContents() {
        return menu.getCrucibleState()
            .map(state -> state.amount() > 0)
            .orElse(false);
    }

    private Component metalName(ResourceLocation metalId) {
        return metalId == null
            ? Component.literal("-")
            : Component.literal(metalId.toString());
    }

    private void drawPlayerInventorySlots(GuiGraphics graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(
                    graphics,
                    PLAYER_SLOTS_X + column * SLOT_SIZE,
                    PLAYER_SLOTS_Y + row * SLOT_SIZE
                );
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(
                graphics,
                PLAYER_SLOTS_X + column * SLOT_SIZE,
                PLAYER_SLOTS_Y + 58
            );
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        drawFrame(graphics, x, y, SLOT_SIZE, SLOT_SIZE);
        graphics.fill(
            leftPos + x + 2,
            topPos + y + 2,
            leftPos + x + SLOT_SIZE - 1,
            topPos + y + SLOT_SIZE - 1,
            0xFF8B8B8B
        );
    }

    private void drawFrame(
        GuiGraphics graphics,
        int x,
        int y,
        int width,
        int height
    ) {
        int left = leftPos + x;
        int top = topPos + y;
        graphics.fill(left, top, left + width, top + 1, BORDER_LIGHT);
        graphics.fill(left, top, left + 1, top + height, BORDER_LIGHT);
        graphics.fill(
            left,
            top + height - 1,
            left + width,
            top + height,
            BORDER_DARK
        );
        graphics.fill(
            left + width - 1,
            top,
            left + width,
            top + height,
            BORDER_DARK
        );
    }
}
