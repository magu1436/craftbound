package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.client.forging.ForgingClientSessionState;
import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.ForgingCompleteRequestPacket;
import com.magu1436.craftbound.network.packet.ForgingFeedbackPacket;
import com.magu1436.craftbound.network.packet.ForgingHeartbeatPacket;
import com.magu1436.craftbound.network.packet.ForgingPauseRequestPacket;
import com.magu1436.craftbound.network.packet.ForgingSessionSyncPacket;
import com.magu1436.craftbound.network.packet.ForgingStrikeRequestPacket;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingGaugeCalculator;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ForgingScreen extends AbstractContainerScreen<ForgingMenu> {
    private static final int PANEL_COLOR = 0xE0202020;
    private static final int BORDER_COLOR = 0xFFB0B0B0;
    private static final int GAUGE_COLOR = 0xFFE0A040;
    private static final int REFERENCE_COLOR = 0xFF40D0FF;

    private int tickCounter;
    private boolean completionPending;
    private Double strikeReference;
    private ForgingFeedbackPacket.Status observedFeedback;

    public ForgingScreen(ForgingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 240;
        imageHeight = 176;
        inventoryLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, PANEL_COLOR);
        outline(graphics, left, top, imageWidth, imageHeight, BORDER_COLOR);

        int strikeX = left + 88;
        int strikeY = top + 34;
        outline(graphics, strikeX, strikeY, 64, 58, BORDER_COLOR);
        renderWorkingPart(graphics, strikeX + 16, strikeY + 13);

        int gaugeX = left + 24;
        int gaugeY = top + 122;
        int gaugeWidth = 192;
        graphics.fill(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + 2, BORDER_COLOR);
        renderMarks(graphics, gaugeX, gaugeY, gaugeWidth);

        ForgingSessionSyncPacket session = ForgingClientSessionState.session();
        if (session != null) {
            double value = currentGaugeValue(session);
            int pointerX = gaugeX + (int) Math.round(value / 100.0D * gaugeWidth);
            graphics.fill(pointerX - 1, gaugeY - 8, pointerX + 2, gaugeY + 8, GAUGE_COLOR);
            if (session.assistSnapshot().showCurrentValue()) {
                graphics.drawCenteredString(font, Integer.toString((int) Math.round(value)), pointerX, gaugeY - 20, 0xFFFFFF);
            }
            if (!session.assistSnapshot().strikeReferenceEnabled()) strikeReference = null;
        }
        if (strikeReference != null) {
            int referenceX = gaugeX + (int) Math.round(strikeReference / 100.0D * gaugeWidth);
            graphics.fill(referenceX, gaugeY - 12, referenceX + 1, gaugeY + 12, REFERENCE_COLOR);
        }

        int buttonX = left + 174;
        int buttonY = top + 148;
        graphics.fill(buttonX, buttonY, buttonX + 50, buttonY + 18,
            completionPending ? 0xFF555555 : 0xFF704820);
        outline(graphics, buttonX, buttonY, 50, 18, BORDER_COLOR);
        graphics.drawCenteredString(font, Component.translatable("screen.craftbound.forging.complete"),
            buttonX + 25, buttonY + 5, 0xFFFFFF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, leftPos + imageWidth / 2, topPos + 10, 0xFFFFFF);
        ForgingFeedbackPacket.Status feedback = ForgingClientSessionState.lastFeedback();
        if (feedback == ForgingFeedbackPacket.Status.ERROR) {
            graphics.drawCenteredString(font, Component.translatable("screen.craftbound.forging.error"),
                leftPos + imageWidth / 2, topPos + 100, 0xFF6060);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        tickCounter++;
        ForgingFeedbackPacket.Status feedback = ForgingClientSessionState.lastFeedback();
        if (feedback != observedFeedback) {
            observedFeedback = feedback;
            if (feedback == ForgingFeedbackPacket.Status.ERROR) completionPending = false;
        }
        ForgingSessionSyncPacket session = ForgingClientSessionState.session();
        if (session != null && tickCounter % 20 == 0) {
            CraftboundNetwork.sendToServer(new ForgingHeartbeatPacket(
                session.sessionId(), ForgingClientSessionState.nextSequence()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ForgingSessionSyncPacket session = ForgingClientSessionState.session();
        if (session == null) return super.mouseClicked(mouseX, mouseY, button);

        if (inside(mouseX, mouseY, leftPos + 174, topPos + 148, 50, 18) && button == 0) {
            if (!completionPending) {
                completionPending = true;
                CraftboundNetwork.sendToServer(new ForgingCompleteRequestPacket(
                    session.sessionId(), ForgingClientSessionState.nextSequence()));
            }
            return true;
        }
        if (inside(mouseX, mouseY, leftPos + 88, topPos + 34, 64, 58) && button == 0) {
            CraftboundNetwork.sendToServer(new ForgingStrikeRequestPacket(
                session.sessionId(), ForgingClientSessionState.nextSequence(),
                ForgingClientSessionState.estimatedServerTick()));
            return true;
        }
        int gaugeX = leftPos + 24;
        int gaugeY = topPos + 112;
        if (inside(mouseX, mouseY, gaugeX, gaugeY, 192, 24)
            && session.assistSnapshot().strikeReferenceEnabled()) {
            if (button == 0) strikeReference = Math.max(0.0D, Math.min(100.0D,
                (mouseX - gaugeX) / 192.0D * 100.0D));
            if (button == 1) strikeReference = null;
            return button == 0 || button == 1;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        ForgingSessionSyncPacket session = ForgingClientSessionState.session();
        ForgingFeedbackPacket.Status feedback = ForgingClientSessionState.lastFeedback();
        if (session != null && feedback != ForgingFeedbackPacket.Status.COMPLETED
            && feedback != ForgingFeedbackPacket.Status.FAILED) {
            CraftboundNetwork.sendToServer(new ForgingPauseRequestPacket(
                session.sessionId(), ForgingClientSessionState.nextSequence(),
                ForgingClientSessionState.estimatedServerTick()));
        }
        strikeReference = null;
        ForgingClientSessionState.clear();
        super.onClose();
    }

    private void renderWorkingPart(GuiGraphics graphics, int x, int y) {
        ItemStack stack = menu.getForgingTable().map(table -> table.getDisplayStack()).orElse(ItemStack.EMPTY);
        if (stack.isEmpty()) return;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 100.0F);
        graphics.pose().scale(2.0F, 2.0F, 2.0F);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    private void renderMarks(GuiGraphics graphics, int x, int y, int width) {
        ForgingSessionSyncPacket session = ForgingClientSessionState.session();
        int interval = session == null ? 25 : session.assistSnapshot().markInterval();
        for (int value = 0; value <= 100; value += interval) {
            if (value % 25 == 0) continue;
            int markX = x + (int) Math.round(value / 100.0D * width);
            graphics.fill(markX, y - 3, markX + 1, y + 5, 0xFF808080);
        }
        for (int value = 0; value <= 100; value += 25) {
            int markX = x + (int) Math.round(value / 100.0D * width);
            graphics.fill(markX, y - 5, markX + 1, y + 7, BORDER_COLOR);
            graphics.drawCenteredString(font, Integer.toString(value), markX, y + 10, 0xFFFFFF);
        }
    }

    private static double currentGaugeValue(ForgingSessionSyncPacket session) {
        return ForgingGaugeCalculator.valueAt(
            session.serverGameTime(), session.baseGaugeValue(), session.baseGaugeDirection(),
            ForgingClientSessionState.estimatedServerTick()
        ).value();
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }
}
