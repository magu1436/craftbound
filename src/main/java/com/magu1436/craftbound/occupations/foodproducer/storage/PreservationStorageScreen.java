package com.magu1436.craftbound.occupations.foodproducer.storage;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** バニラの3段チェスト画像を使う保存設備画面。 */
public final class PreservationStorageScreen extends AbstractContainerScreen<PreservationStorageMenu> {

    private static final ResourceLocation CONTAINER_TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/gui/container/generic_54.png"
    );
    private static final int STORAGE_ROWS = 3;
    private static final int STORAGE_AREA_HEIGHT = STORAGE_ROWS * 18 + 17;

    public PreservationStorageScreen(
            PreservationStorageMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);
        imageHeight = 114 + STORAGE_ROWS * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, CONTAINER_TEXTURE);
        graphics.blit(
                CONTAINER_TEXTURE,
                leftPos,
                topPos,
                0,
                0,
                imageWidth,
                STORAGE_AREA_HEIGHT
        );
        graphics.blit(
                CONTAINER_TEXTURE,
                leftPos,
                topPos + STORAGE_AREA_HEIGHT,
                0,
                126,
                imageWidth,
                96
        );
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        Component multiplier = Component.translatable(
                "screen.craftbound.preservation_storage.multiplier",
                menu.preservationMultiplier()
        );
        graphics.drawString(
                font,
                multiplier,
                imageWidth - 8 - font.width(multiplier),
                titleLabelY,
                0x404040,
                false
        );
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                inventoryLabelY,
                0x404040,
                false
        );
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
