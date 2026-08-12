package com.magu1436.craftbound.occupations.foodproducer.storage;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** シュルカーボックス同様に内部在庫を保持し、他の携帯コンテナへ入れられない保存設備。 */
public final class PreservationStorageBlockItem extends BlockItem {

    public PreservationStorageBlockItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        int stored = PreservationStorageItemData.storedItemCount(stack);
        if (stored > 0) {
            tooltip.add(Component.translatable(
                    "tooltip.craftbound.preservation_storage.contents",
                    stored
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
