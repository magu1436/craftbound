package com.magu1436.craftbound.occupations.blacksmith.client.casting;

import com.magu1436.craftbound.registry.CraftboundItems;
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
        Minecraft.getInstance().getItemRenderer().renderStatic(
            new ItemStack(CraftboundItems.BLANK_MOLD.get()),
            ItemDisplayContext.NONE,
            packedLight,
            packedOverlay,
            poseStack,
            buffers,
            Minecraft.getInstance().level,
            0
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
