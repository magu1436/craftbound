package com.magu1436.craftbound.occupations.blacksmith.client;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.common.quality.QualityStateService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT
)
public final class BlacksmithQualityTooltipEvents {
    private BlacksmithQualityTooltipEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        QualityStateService.read(event.getItemStack())
            .flatMap(state -> ClientBlacksmithQualityTierDefinitions.resolve(state.quality()))
            .ifPresent(tier -> event.getToolTip().add(
                Component.translatable(
                    "tooltip.craftbound.blacksmith_quality",
                    Component.translatable(tier.translationKey())
                ).withStyle(ChatFormatting.GRAY)
            ));
    }
}
