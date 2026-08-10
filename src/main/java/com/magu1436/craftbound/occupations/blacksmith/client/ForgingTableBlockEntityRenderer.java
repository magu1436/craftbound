package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ForgingTableBlockEntityRenderer implements BlockEntityRenderer<ForgingTableBlockEntity> {
    public ForgingTableBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ForgingTableBlockEntity table, float partialTick, PoseStack poseStack,
        MultiBufferSource buffers, int packedLight, int packedOverlay) {
        ItemStack stack = table.getDisplayStack();
        if (stack.isEmpty()) return;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.02D, 0.5D);
        if (table.getBlockState().hasProperty(ForgingTableBlock.FACING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-table.getBlockState().getValue(ForgingTableBlock.FACING).toYRot()));
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.6F, 0.6F, 0.6F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
            packedLight, packedOverlay, poseStack, buffers, table.getLevel(), 0);
        poseStack.popPose();
    }
}
