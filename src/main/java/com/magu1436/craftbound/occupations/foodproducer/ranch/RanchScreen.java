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
        imageHeight = 239;
        inventoryLabelY = 146;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addRenderableWidget(Button.builder(
                Component.literal("<"),
                button -> selectTarget(menu.getTarget().previous())
        ).bounds(leftPos + 27, topPos + 38, 24, 20).build());
        nextButton = addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> selectTarget(menu.getTarget().next())
        ).bounds(leftPos + 125, topPos + 38, 24, 20).build());
        updateButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.craftbound.ranch_block.update"),
                button -> updatePerformance()
        ).bounds(leftPos + 8, topPos + 104, 54, 18).build());
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
        graphics.fill(leftPos + 79, topPos + 61, leftPos + 99, topPos + 81, 0xFF373737);
        graphics.fill(leftPos + 80, topPos + 62, leftPos + 98, topPos + 80, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        Component target = Component.translatable(menu.getTarget().translationKey());
        int targetX = (imageWidth - font.width(target)) / 2;
        graphics.drawString(font, target, targetX, 43, 0x404040, false);
        Component feedLabel = Component.translatable("screen.craftbound.ranch_block.feed");
        graphics.drawString(font, feedLabel, 89 - font.width(feedLabel) / 2, 82, 0x404040, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.feed_saving",
                        menu.getFeedManagementRank() * 10
                ),
                8,
                18,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.capacity",
                        menu.getManagementCapacity()
                ),
                8,
                28,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.animals",
                        menu.getManagedCount(),
                        menu.getRegisteredCount(),
                        menu.getManagementCapacity()
                ),
                67,
                104,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.range_animals",
                        menu.getRangeCount(),
                        menu.getOverflowCount()
                ),
                67,
                114,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                Component.translatable(
                        "screen.craftbound.ranch_block.age_counts",
                        menu.getAdultCount(),
                        menu.getChildCount(),
                        menu.getBreedableCount()
                ),
                8,
                126,
                0x404040,
                false
        );
        Component nextFeed = menu.getNextFeedSeconds() < 0
                ? Component.translatable("screen.craftbound.ranch_block.next_feed.none")
                : Component.translatable(
                        "screen.craftbound.ranch_block.next_feed",
                        menu.getNextFeedSeconds()
                );
        graphics.drawString(font, nextFeed, 8, 136, 0x404040, false);
        graphics.drawString(
                font,
                Component.translatable("screen.craftbound.ranch_block.range"),
                8,
                92,
                0x404040,
                false
        );
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
