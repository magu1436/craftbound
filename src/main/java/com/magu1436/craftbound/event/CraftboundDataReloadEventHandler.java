package com.magu1436.craftbound.event;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineClearEffectDefinitions;
import com.magu1436.craftbound.occupations.adventurer.data.DeathlineExcludedDamageDefinitions;
import com.magu1436.craftbound.occupations.explorer.data.ToolCareBlockDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettingsDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.CarvingShapeDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.assembly.QualityAssemblyRecipeValidationListener;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * サーバーデータのリロードリスナーを登録する。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class CraftboundDataReloadEventHandler {
    private CraftboundDataReloadEventHandler() {
    }

    @SubscribeEvent
    public static void addReloadListeners(
        AddReloadListenerEvent event
    ) {
        event.addListener(DeathlineClearEffectDefinitions.INSTANCE);
        event.addListener(
            DeathlineExcludedDamageDefinitions.INSTANCE
        );
        event.addListener(ToolCareBlockDefinitions.INSTANCE);
        event.addListener(MetalMaterialDefinitions.INSTANCE);
        event.addListener(MetalPartDefinitions.INSTANCE);
        event.addListener(NonMetalMaterialDefinitions.INSTANCE);
        event.addListener(NonMetalPartDefinitions.INSTANCE);
        event.addListener(CarvingShapeDefinitions.INSTANCE);
        event.addListener(BlacksmithSettingsDefinitions.INSTANCE);
        event.addListener(BlacksmithSkillAssistDefinitions.INSTANCE);
        event.addListener(new QualityAssemblyRecipeValidationListener(
            event.getServerResources().getRecipeManager()));
    }
}
