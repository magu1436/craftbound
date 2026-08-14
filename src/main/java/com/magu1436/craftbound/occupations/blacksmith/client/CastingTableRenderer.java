package com.magu1436.craftbound.occupations.blacksmith.client;

import java.util.Optional;

import com.magu1436.craftbound.occupations.blacksmith.casting.CastingProcess;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlock;
import com.magu1436.craftbound.occupations.blacksmith.casting.CastingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.client.casting.CastingMaskDefinition;
import com.magu1436.craftbound.occupations.blacksmith.client.casting.CastingMaskRegistry;
import com.magu1436.craftbound.occupations.blacksmith.client.casting.CastingMaskRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class CastingTableRenderer
    implements BlockEntityRenderer<CastingTableBlockEntity> {

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
            renderMold(
                mold,
                table.getActiveProcessForRendering(),
                poseStack,
                buffers,
                displayLight,
                packedOverlay
            );
        }
        poseStack.popPose();
    }

    private static void renderMold(
        ItemStack mold,
        CastingProcess process,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.79D, 0.0D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.62F, 0.62F, 0.62F);
        CastingMaskRenderer.renderBaseMold(
            poseStack,
            buffers,
            packedLight,
            packedOverlay
        );
        ResourceLocation moldItemId = ForgeRegistries.ITEMS.getKey(mold.getItem());
        Optional<CastingMaskDefinition> definition =
            CastingMaskRegistry.findByMold(moldItemId);
        definition.ifPresent(mask -> {
            CastingMaskRenderer.renderEmptyMask(
                mask,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
            );
            if (process != null) {
                CastingMaskRenderer.renderMetalFill(
                    mask,
                    MetalRenderColorResolver.resolve(process.visualData()),
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
                );
            }
        });
        poseStack.popPose();
    }

    private static void rotateToFacing(BlockState state, PoseStack poseStack) {
        if (state.hasProperty(CastingTableBlock.FACING)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(
                -state.getValue(CastingTableBlock.FACING).toYRot()
            ));
        }
    }
}
