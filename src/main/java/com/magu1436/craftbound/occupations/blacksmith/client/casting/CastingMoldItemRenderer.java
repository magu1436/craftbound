package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class CastingMoldItemRenderer
    extends BlockEntityWithoutLevelRenderer {

    private static CastingMoldItemRenderer instance;

    private CastingMoldItemRenderer() {
        super(
            Minecraft.getInstance().getBlockEntityRenderDispatcher(),
            Minecraft.getInstance().getEntityModels()
        );
    }

    public static CastingMoldItemRenderer getInstance() {
        if (instance == null) {
            instance = new CastingMoldItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        CastingMaskRenderer.renderBaseMold(
            poseStack,
            buffers,
            packedLight,
            packedOverlay
        );

        ResourceLocation moldItemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        CastingMaskRegistry.findByMold(moldItemId).ifPresent(definition ->
            CastingMaskRenderer.renderEmptyMask(
                definition,
                poseStack,
                buffers,
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        );
    }
}
