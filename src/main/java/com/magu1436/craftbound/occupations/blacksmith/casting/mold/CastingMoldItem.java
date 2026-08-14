package com.magu1436.craftbound.occupations.blacksmith.casting.mold;

import java.util.function.Consumer;

import com.magu1436.craftbound.occupations.blacksmith.client.casting.CastingMoldItemRenderer;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class CastingMoldItem extends Item {
    public CastingMoldItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return CastingMoldItemRenderer.getInstance();
            }
        });
    }
}
