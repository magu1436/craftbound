package com.magu1436.craftbound.occupations.blacksmith.experience;

import com.magu1436.craftbound.occupations.blacksmith.experience.BlacksmithProcessExperienceSource.AwardResult;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** 実際に付与された鍛冶経験値をプレイヤーへ通知する。 */
public final class BlacksmithExperienceNotifier {
    private static final String MESSAGE_KEY =
        "message.craftbound.blacksmith.experience_gained";

    private BlacksmithExperienceNotifier() {
    }

    public static void notifyGranted(
        ServerPlayer player,
        AwardResult result
    ) {
        if (!result.successful() || result.grantedExperience() <= 0) {
            return;
        }
        player.displayClientMessage(
            Component.translatable(
                MESSAGE_KEY,
                result.grantedExperience()
            ),
            true
        );
    }
}
