package com.magu1436.craftbound.occupations.foodproducer.ranch;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** 専用画像を必要としない、牧畜ブロック基盤用の最小GUI。 */
public final class RanchScreen extends AbstractContainerScreen<RanchMenu> {

    private Button previousButton;
    private Button nextButton;
    private Button updateButton;

    public RanchScreen(RanchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 195;
        inventoryLabelY = 101;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> selectTarget(menu.getTarget().previous())
        ).bounds(leftPos + 27, topPos + 29, 24, 20).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> selectTarget(menu.getTarget().next())
        ).bounds(leftPos + 125, topPos + 29, 24, 20).build());
        updateButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.craftbound.ranch_block.update"),
                button -> updatePerformance()
        ).bounds(leftPos + 8, topPos + 74, 54, 18).build());
        updateButtonState();
    }

    private void updatePerformance() {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(
                    menu.containerId,
                    RanchMenu.UPDATE_PERFORMANCE_BUTTON
            );
        }
    }

    private void selectTarget(RanchTarget target) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, target.id());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtonState();
    }

    private void updateButtonState() {
        boolean active = menu.canChangeTarget();
        previousButton.active = active;
        nextButton.active = active;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF8B8B8B);
        graphics.fill(leftPos + 5, topPos + 5, leftPos + imageWidth - 5, topPos + imageHeight - 5, 0xFFC6C6C6);
        graphics.fill(leftPos + 79, topPos + 43, leftPos + 99, topPos + 63, 0xFF373737);
        graphics.fill(leftPos + 80, topPos + 44, leftPos + 98, topPos + 62, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        Component target = Component.translatable(menu.getTarget().translationKey());
        int targetX = (imageWidth - font.width(target)) / 2;
        graphics.drawString(font, target, targetX, 34, 0x404040, false);
        graphics.drawString(
                font,
                Component.translatable("screen.craftbound.ranch_block.feed"),
                64,
                63,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.ranks",
                        menu.getFeedManagementRank(),
                        menu.getRanchCapacityRank(),
                        menu.getManagementCapacity()
                ),
                8,
                19,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.animals",
                        menu.getManagedCount(),
                        menu.getManagementCapacity(),
                        menu.getAdultCount(),
                        menu.getChildCount()
                ),
                67,
                73,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.breeding",
                        menu.getBreedableCount()
                ),
                67,
                83,
                0x404040,
                false
        );
        Component nextFeed = menu.getNextFeedSeconds() < 0
                ? Component.translatable("screen.craftbound.ranch_block.next_feed.none")
                : Component.translatable(
                        "screen.craftbound.ranch_block.next_feed",
                        menu.getNextFeedSeconds()
                );
        graphics.drawString(font, nextFeed, 67, 93, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
