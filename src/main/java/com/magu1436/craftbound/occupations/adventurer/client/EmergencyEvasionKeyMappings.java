package com.magu1436.craftbound.occupations.adventurer.client;

import com.magu1436.craftbound.Craftbound;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.lwjgl.glfw.GLFW;

/**
 * 冒険家のクライアントキー割り当て。
 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class EmergencyEvasionKeyMappings {
    private static final String CATEGORY_KEY =
        "key.categories.craftbound";
    private static final String EMERGENCY_EVASION_KEY =
        "key.craftbound.emergency_evasion";

    public static final Lazy<KeyMapping> EMERGENCY_EVASION =
        Lazy.of(
            () -> new KeyMapping(
                EMERGENCY_EVASION_KEY,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY_KEY
            )
        );

    private EmergencyEvasionKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(EMERGENCY_EVASION.get());
    }
}
