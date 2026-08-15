package com.magu1436.craftbound.client.carving;

import com.magu1436.craftbound.occupations.blacksmith.carving.logic.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.menu.CarvingMenu;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import com.magu1436.craftbound.network.packet.CarvingSessionSyncPacket;

public final class CarvingScreen extends AbstractContainerScreen<CarvingMenu> {
    private static final int GRID_PIXELS = 128;
    private boolean carving; private UUID strokeId; private int lastX, lastY; private long lastSendTick;
    private int heartbeatTicks;
    private int warningTicks; private long observedFeedback;
    public CarvingScreen(CarvingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageWidth = 220; imageHeight = 190;
    }
    @Override protected void init() {
        super.init(); addRenderableWidget(Button.builder(Component.translatable("button.craftbound.carving.complete"), value -> {
            if (minecraft != null && minecraft.gameMode != null)
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, CarvingMenu.COMPLETE_BUTTON);
        }).bounds(leftPos + 145, topPos + 160, 65, 20).build());
    }
    @Override protected void containerTick() {
        super.containerTick();
        if (++heartbeatTicks >= 20) { heartbeatTicks = 0; CarvingClientSessionState.heartbeat(); }
        if (warningTicks > 0) warningTicks--;
        if (observedFeedback != CarvingClientSessionState.feedbackSequence()) {
            observedFeedback = CarvingClientSessionState.feedbackSequence();
            if (CarvingClientSessionState.feedback() == com.magu1436.craftbound.network.packet.CarvingFeedbackPacket.Status.WARNING)
                warningTicks = 8;
        }
        if (carving && minecraft != null && minecraft.level != null
            && minecraft.level.getGameTime() - lastSendTick >= 2L) send(lastX, lastY);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int[] cell = cell(mouseX, mouseY);
        if (button == 0 && cell != null) {
            carving = true; strokeId = UUID.randomUUID(); lastX = cell[0]; lastY = cell[1];
            send(lastX, lastY); return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        int[] cell = cell(mouseX, mouseY);
        if (carving && button == 0 && cell != null && (cell[0] != lastX || cell[1] != lastY)) {
            CarvingGrid grid = CarvingClientSessionState.predictedGrid();
            if (grid != null) CarvingClientSessionState.sendStroke(strokeId,
                new CarvingStroke(lastX, lastY, cell[0], cell[1], true));
            lastX = cell[0]; lastY = cell[1]; updateSendTick(); return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) { carving = false; strokeId = null; }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private void send(int x, int y) {
        if (CarvingClientSessionState.predictedGrid() != null)
            CarvingClientSessionState.sendStroke(strokeId, new CarvingStroke(x, y, x, y, false));
        updateSendTick();
    }
    private void updateSendTick() {
        lastSendTick = minecraft == null || minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }
    private int[] cell(double mouseX, double mouseY) {
        CarvingGrid grid = CarvingClientSessionState.predictedGrid(); if (grid == null) return null;
        int gx = leftPos + 10, gy = topPos + 25;
        if (mouseX < gx || mouseY < gy || mouseX >= gx + GRID_PIXELS || mouseY >= gy + GRID_PIXELS) return null;
        return new int[] { Math.min(grid.size() - 1, (int) ((mouseX - gx) * grid.size() / GRID_PIXELS)),
            Math.min(grid.size() - 1, (int) ((mouseY - gy) * grid.size() / GRID_PIXELS)) };
    }
    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF202020);
        CarvingGrid grid = CarvingClientSessionState.predictedGrid(); if (grid == null) return;
        int gx = leftPos + 10, gy = topPos + 25;
        CarvingSessionSyncPacket session = CarvingClientSessionState.session();
        CarvingMaterialColorMap colors = session == null ? null
            : DefaultCarvingMaterialTextureProvider.INSTANCE.colorMap(session.material(), session.carvingTexture());
        for (int y = 0; y < grid.size(); y++) for (int x = 0; x < grid.size(); x++) {
            int x0 = gx + x * GRID_PIXELS / grid.size(), x1 = gx + (x + 1) * GRID_PIXELS / grid.size();
            int y0 = gy + y * GRID_PIXELS / grid.size(), y1 = gy + (y + 1) * GRID_PIXELS / grid.size();
            int alpha = (int) Math.round(grid.get(x, y) * 255.0D);
            int rgb = colors == null ? 0xA07040 : colors.color(x * colors.size() / grid.size(), y * colors.size() / grid.size());
            graphics.fill(x0, y0, x1, y1, (alpha << 24) | rgb);
        }
        int border = warningTicks > 0 ? 0xFFE07040 : 0xFFB0B0B0;
        outline(graphics, gx - 1, gy - 1, GRID_PIXELS + 2, GRID_PIXELS + 2, border);
        int[] hover = cell(mouseX, mouseY);
        if (hover != null) renderBrushPreview(graphics, grid, hover[0], hover[1], gx, gy);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); super.render(graphics, mouseX, mouseY, partialTick);
        CarvingSessionSyncPacket session = CarvingClientSessionState.session();
        if (session != null) {
            Component header = Component.empty().append(session.material().getHoverName()).append("  →  ")
                .append(session.output().getHoverName());
            graphics.drawCenteredString(font, header, leftPos + imageWidth / 2, topPos + 8, 0xFFFFFF);
            graphics.renderItem(session.material(), leftPos + 31, topPos + 4);
        }
    }
    @Override public void onClose() { CarvingClientSessionState.clear(); super.onClose(); }
    private void renderBrushPreview(GuiGraphics graphics, CarvingGrid grid, int x, int y, int gx, int gy) {
        CarvingSessionSyncPacket session = CarvingClientSessionState.session(); if (session == null) return;
        for (CarvingBrush.Cell cell : new CarvingBrush().cells(x, y, grid.size(), session.brushRadius())) {
            int x0 = gx + cell.x() * GRID_PIXELS / grid.size(), x1 = gx + (cell.x() + 1) * GRID_PIXELS / grid.size();
            int y0 = gy + cell.y() * GRID_PIXELS / grid.size(), y1 = gy + (cell.y() + 1) * GRID_PIXELS / grid.size();
            outline(graphics, x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0), 0xA0FFFFFF);
        }
    }
    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color); graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
