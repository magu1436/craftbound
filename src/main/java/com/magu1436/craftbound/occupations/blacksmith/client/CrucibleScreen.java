package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.crucible.CrucibleState;
import com.magu1436.craftbound.occupations.blacksmith.crucible.menu.CrucibleMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CrucibleScreen
    extends AbstractContainerScreen<CrucibleMenu> {

    private static final int BACKGROUND_COLOR = 0xFFC6C6C6;
    private static final int BORDER_LIGHT = 0xFFFFFFFF;
    private static final int BORDER_DARK = 0xFF555555;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_SLOT_X = 26;
    private static final int INPUT_SLOT_Y = 35;
    private static final int PLAYER_SLOTS_X = 7;
    private static final int PLAYER_SLOTS_Y = 83;

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
        Button discardButton = Button.builder(
            Component.translatable("screen.craftbound.crucible.discard"),
            button -> {
            }
        ).bounds(leftPos + 112, topPos + 33, 52, 20).build();
        discardButton.active = false;
        addRenderableWidget(discardButton);
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
        graphics.drawString(
            font,
            Component.translatable("screen.craftbound.crucible.metal", "-"),
            52,
            27,
            0x404040,
            false
        );
        graphics.drawString(
            font,
            Component.translatable(
                "screen.craftbound.crucible.amount",
                0,
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
                Component.translatable("screen.craftbound.crucible.state.empty")
            ),
            52,
            53,
            0x404040,
            false
        );
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
