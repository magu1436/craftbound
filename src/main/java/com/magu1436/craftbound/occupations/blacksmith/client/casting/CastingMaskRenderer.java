package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class CastingMaskRenderer {
    private static final int MASK_SIZE = 16;
    private static final ResourceLocation OVERLAY_TEXTURE =
        new ResourceLocation("minecraft", "block/white_concrete");

    private CastingMaskRenderer() {
    }

    public static void renderEmptyMask(
        CastingMaskDefinition definition,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
            .apply(OVERLAY_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(
            RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
        );
        renderSpans(
            definition.geometry().rimSpans(),
            CastingMaskRenderStyle.RIM_OFFSET,
            CastingMaskRenderStyle.RIM_RED,
            CastingMaskRenderStyle.RIM_GREEN,
            CastingMaskRenderStyle.RIM_BLUE,
            CastingMaskRenderStyle.RIM_ALPHA,
            poseStack,
            consumer,
            sprite,
            packedLight,
            packedOverlay
        );
        renderSpans(
            definition.geometry().cavitySpans(),
            CastingMaskRenderStyle.CAVITY_OFFSET,
            CastingMaskRenderStyle.CAVITY_RED,
            CastingMaskRenderStyle.CAVITY_GREEN,
            CastingMaskRenderStyle.CAVITY_BLUE,
            CastingMaskRenderStyle.CAVITY_ALPHA,
            poseStack,
            consumer,
            sprite,
            packedLight,
            packedOverlay
        );
    }

    private static void renderSpans(
        Iterable<MaskSpan> spans,
        float depth,
        int red,
        int green,
        int blue,
        int alpha,
        PoseStack poseStack,
        VertexConsumer consumer,
        TextureAtlasSprite sprite,
        int packedLight,
        int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();
        for (MaskSpan span : spans) {
            float left = normalizedColumn(span.startColumn());
            float right = normalizedColumn(span.endColumnExclusive());
            float top = normalizedRow(span.row());
            float bottom = normalizedRow(span.row() + 1);
            quad(
                consumer,
                position,
                normal,
                left,
                right,
                top,
                bottom,
                depth,
                red,
                green,
                blue,
                alpha,
                sprite,
                packedOverlay,
                packedLight
            );
        }
    }

    private static float normalizedColumn(int column) {
        return -0.5F + (float) column / MASK_SIZE;
    }

    private static float normalizedRow(int row) {
        return 0.5F - (float) row / MASK_SIZE;
    }

    private static void quad(
        VertexConsumer consumer,
        Matrix4f position,
        Matrix3f normal,
        float left,
        float right,
        float top,
        float bottom,
        float depth,
        int red,
        int green,
        int blue,
        int alpha,
        TextureAtlasSprite sprite,
        int packedOverlay,
        int packedLight
    ) {
        vertex(consumer, position, normal, left, bottom, depth, red, green,
            blue, alpha, sprite.getU0(), sprite.getV1(), packedOverlay, packedLight);
        vertex(consumer, position, normal, right, bottom, depth, red, green,
            blue, alpha, sprite.getU1(), sprite.getV1(), packedOverlay, packedLight);
        vertex(consumer, position, normal, right, top, depth, red, green,
            blue, alpha, sprite.getU1(), sprite.getV0(), packedOverlay, packedLight);
        vertex(consumer, position, normal, left, top, depth, red, green,
            blue, alpha, sprite.getU0(), sprite.getV0(), packedOverlay, packedLight);
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
        int alpha,
        float u,
        float v,
        int packedOverlay,
        int packedLight
    ) {
        consumer.vertex(position, x, y, z)
            .color(red, green, blue, alpha)
            .uv(u, v)
            .overlayCoords(packedOverlay)
            .uv2(packedLight)
            .normal(normal, 0.0F, 0.0F, 1.0F)
            .endVertex();
    }
}
