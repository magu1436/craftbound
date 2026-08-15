package com.magu1436.craftbound.client.carving;

import com.magu1436.craftbound.occupations.blacksmith.carving.menu.CarvingPartSelectionMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CarvingPartSelectionScreen extends AbstractContainerScreen<CarvingPartSelectionMenu> {
    public CarvingPartSelectionScreen(CarvingPartSelectionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = Math.max(62, 38 + menu.candidates().size() * 24);
    }
    @Override protected void init() {
        super.init(); int y = topPos + 30;
        for (int i = 0; i < menu.candidates().size(); i++) {
            int button = i;
            addRenderableWidget(Button.builder(menu.candidates().get(i).output().getHoverName(), value -> {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
            }).bounds(leftPos + 20, y, 180, 20).build());
            y += 24;
        }
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
    }
    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, imageWidth / 2, 10, 0xFFFFFF);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
    }
}
