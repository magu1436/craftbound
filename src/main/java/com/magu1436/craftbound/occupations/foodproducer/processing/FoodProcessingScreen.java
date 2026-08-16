package com.magu1436.craftbound.occupations.foodproducer.processing;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/** 専用画像を必要としない、初期加工設備の最小GUI。 */
public final class FoodProcessingScreen extends AbstractContainerScreen<FoodProcessingMenu> {

    private static final int BUTTON_Y = 103;
    private static final int STATUS_TOP = 80;
    private static final int STATUS_WIDTH = 160;

    private Button cutButton;
    private Button mixButton;
    private Button startButton;

    public FoodProcessingScreen(FoodProcessingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 219;
        inventoryLabelY = 125;
    }

    @Override
    protected void init() {
        super.init();
        cutButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.craftbound.processing.cut"),
                button -> send(FoodProcessingMenu.SELECT_CUT_BUTTON)
        ).bounds(leftPos + 8, topPos + BUTTON_Y, 42, 18).build());
        mixButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.craftbound.processing.mix"),
                button -> send(FoodProcessingMenu.SELECT_MIX_BUTTON)
        ).bounds(leftPos + 52, topPos + BUTTON_Y, 42, 18).build());
        startButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.craftbound.processing.start"),
                button -> send(FoodProcessingMenu.START_BUTTON)
        ).bounds(leftPos + 116, topPos + BUTTON_Y, 52, 18).build());
        updateButtons();
    }

    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        boolean table = menu.station() == FoodProcessingStation.COOKING_TABLE;
        cutButton.visible = table;
        mixButton.visible = table;
        cutButton.active = table && !menu.running() && menu.operation() != FoodProcessingOperation.CUT;
        mixButton.active = table && !menu.running() && menu.operation() != FoodProcessingOperation.MIX;
        startButton.active = !menu.running();
        startButton.setX(leftPos + (table ? 116 : 62));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF8B8B8B);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFFC6C6C6);
        for (int x : new int[] { 26, 44, 62, 116, 134 }) drawSlot(graphics, x, 36);
        drawSlot(graphics, 44, 61);
        if (menu.station().usesFuel()) {
            drawSlot(graphics, 80, 61);
            int flameHeight = menu.burnTotal() <= 0 ? 0 : Math.min(12,
                    Math.round(12.0F * menu.burnTime() / menu.burnTotal()));
            graphics.fill(leftPos + 99, topPos + 62 + (12 - flameHeight),
                    leftPos + 105, topPos + 74, 0xFFFF8C00);
        }
        int width = menu.totalTicks() <= 0 ? 0 : Math.min(50,
                Math.round(50.0F * menu.progress() / menu.totalTicks()));
        graphics.fill(leftPos + 108, topPos + 62, leftPos + 160, topPos + 69, 0xFF373737);
        graphics.fill(leftPos + 109, topPos + 63, leftPos + 109 + width, topPos + 68, 0xFF55AA55);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font,
                Component.translatable("screen.craftbound.processing.operation." + menu.operation().serializedName()),
                8, 19, 0x404040, false);
        Component state;
        int stateColor;
        if (menu.running()) {
            state = Component.translatable("screen.craftbound.processing.progress",
                    Math.max(0, (menu.totalTicks() - menu.progress() + 19) / 20));
            stateColor = 0x404040;
        } else if (menu.startStatus() != FoodProcessingBlockEntity.START_STATUS_NONE) {
            state = menu.startStatusMessage();
            stateColor = 0xAA0000;
        } else {
            state = Component.translatable("screen.craftbound.processing.ready");
            stateColor = 0x404040;
        }
        var stateLines = font.split(state, STATUS_WIDTH);
        int stateY = STATUS_TOP + (stateLines.size() == 1 ? 5 : 0);
        for (FormattedCharSequence line : stateLines) {
            graphics.drawString(font, line, imageWidth / 2 - font.width(line) / 2, stateY, stateColor, false);
            stateY += 10;
        }
        if (menu.station().usesFuel()) {
            Component fuel = Component.translatable("screen.craftbound.processing.fuel");
            graphics.drawString(font, fuel, 88 - font.width(fuel) / 2, 52, 0x404040, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
