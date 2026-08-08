package com.magu1436.craftbound.occupations.explorer.notification;

import com.magu1436.craftbound.occupations.explorer.discovery.DiscoveryTarget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** 発見結果をプレイヤーへ通知する。 */
public final class ExplorerDiscoveryNotifier {
    public void notify(
        ServerPlayer player,
        DiscoveryTarget target,
        boolean includeExperience
    ) {
        Component displayName = displayName(target);
        String key = "message.craftbound.explorer.discovery."
            + target.type().name().toLowerCase() + "."
            + (includeExperience
                ? "with_experience" : "without_experience");
        Component message = includeExperience
            ? Component.translatableWithFallback(
                key, "[Exploration] Discovered %1$s (+%2$s XP)",
                displayName, target.xp()
            )
            : Component.translatableWithFallback(
                key, "[Exploration] Discovered %1$s", displayName
            );
        player.sendSystemMessage(
            message.copy().withStyle(ChatFormatting.AQUA)
        );
        player.playNotifySound(
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.PLAYERS,
            0.6F,
            1.0F
        );
    }

    private static Component displayName(DiscoveryTarget target) {
        String configuredKey = null;
        if (target instanceof DiscoveryTarget.Biome biome) {
            configuredKey = biome.translationKey();
        } else if (target instanceof DiscoveryTarget.Dimension dimension) {
            configuredKey = dimension.translationKey();
        } else if (target instanceof DiscoveryTarget.Structure structure) {
            configuredKey = structure.translationKey();
        }
        ResourceLocation id = target.targetId();
        if (configuredKey != null) {
            return Component.translatableWithFallback(
                configuredKey, id.toString()
            );
        }
        String prefix = switch (target.type()) {
            case BIOME -> "biome.";
            case DIMENSION -> "dimension.";
            case STRUCTURE -> "structure.";
        };
        return Component.translatableWithFallback(
            prefix + id.getNamespace() + "." + id.getPath(), id.toString()
        );
    }
}
