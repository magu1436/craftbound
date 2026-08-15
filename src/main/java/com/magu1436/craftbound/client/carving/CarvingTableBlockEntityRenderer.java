package com.magu1436.craftbound.client.carving;

import com.magu1436.craftbound.occupations.blacksmith.carving.CarvingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.world.item.*;

public final class CarvingTableBlockEntityRenderer implements BlockEntityRenderer<CarvingTableBlockEntity> {
    public CarvingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(CarvingTableBlockEntity table, float partialTick, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = table.displayMaterial(); if (stack.isEmpty()) return;
        pose.pushPose(); pose.translate(.5D, 1.02D, .5D); pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.scale(.65F, .65F, .65F);
        int displayLight = table.getLevel() == null ? light
            : LevelRenderer.getLightColor(table.getLevel(), table.getBlockPos().above());
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
            displayLight, overlay, pose, buffers, table.getLevel(), 0); pose.popPose();
    }
}
