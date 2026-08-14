package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.occupations.blacksmith.casting.CastingProcess;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class CastingTableRenderer
    implements BlockEntityRenderer<CastingTableBlockEntity> {

    private static final ResourceLocation SURFACE_TEXTURE =
        new ResourceLocation("craftbound", "block/casting_metal_surface");
    private static final float SURFACE_MIN = -0.29F;
    private static final float SURFACE_MAX = 0.29F;
    private static final float SURFACE_HEIGHT = 0.815F;

    public CastingTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
        CastingTableBlockEntity table,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        int displayLight = table.getLevel() == null
            ? packedLight
            : LevelRenderer.getLightColor(
                table.getLevel(),
                table.getBlockPos().above()
            );
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        rotateToFacing(table.getBlockState(), poseStack);

        ItemStack mold = table.getMoldForRendering();
        if (!mold.isEmpty()) {
            renderMold(table, mold, poseStack, buffers, displayLight, packedOverlay);
        }

        CastingProcess process = table.getActiveProcessForRendering();
        if (process != null) {
            renderMetalSurface(
                process,
                poseStack,
                buffers,
                displayLight,
                packedOverlay
            );
        }
        poseStack.popPose();
    }

    private static void renderMold(
        CastingTableBlockEntity table,
        ItemStack mold,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.79D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.scale(0.62F, 0.62F, 0.62F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            mold,
            ItemDisplayContext.FIXED,
            packedLight,
            packedOverlay,
            poseStack,
            buffers,
            table.getLevel(),
            0
        );
        poseStack.popPose();
    }

    private static void renderMetalSurface(
        CastingProcess process,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        int color = MetalRenderColorResolver.resolve(process.visualData());
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(SURFACE_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(
            RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)
        );
        PoseStack.Pose pose = poseStack.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, position, normal, SURFACE_MIN, SURFACE_HEIGHT,
            SURFACE_MIN, red, green, blue, sprite.getU0(), sprite.getV0(),
            packedOverlay, packedLight);
        vertex(consumer, position, normal, SURFACE_MIN, SURFACE_HEIGHT,
            SURFACE_MAX, red, green, blue, sprite.getU0(), sprite.getV1(),
            packedOverlay, packedLight);
        vertex(consumer, position, normal, SURFACE_MAX, SURFACE_HEIGHT,
            SURFACE_MAX, red, green, blue, sprite.getU1(), sprite.getV1(),
            packedOverlay, packedLight);
        vertex(consumer, position, normal, SURFACE_MAX, SURFACE_HEIGHT,
            SURFACE_MIN, red, green, blue, sprite.getU1(), sprite.getV0(),
            packedOverlay, packedLight);
    }

    private static void vertex(
        VertexConsumer consumer,
        Matrix4f position,
        Matrix3f normal,
        float x,
        float y,
        float z,
        int red,
        int green,
        int blue,
        float u,
        float v,
        int packedOverlay,
        int packedLight
    ) {
        consumer.vertex(position, x, y, z)
            .color(red, green, blue, 255)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(normal, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    private static void rotateToFacing(BlockState state, PoseStack poseStack) {
        if (state.hasProperty(CastingTableBlock.FACING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                -state.getValue(CastingTableBlock.FACING).toYRot()
            ));
        }
    }
}
