package com.magu1436.craftbound.occupations.blacksmith.forging;

import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.ForgingFeedbackPacket;
import com.magu1436.craftbound.network.packet.ForgingSessionSyncPacket;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettings;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettingsDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistResolver;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressStateService;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry.SessionBinding;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.ForgingAssistSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.ForgingSessionState;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class ForgingPacketHandler {
    private ForgingPacketHandler() {}

    public static void strike(ServerPlayer player, UUID sessionId, long sequence, long requestedTick) {
        Optional<Validated> validated = validate(player, sessionId, sequence);
        if (validated.isEmpty()) return;
        Validated value = validated.get();
        long now = player.serverLevel().getGameTime();
        if (requestedTick > now || requestedTick < value.binding().session().baseServerTick()) return;
        long evaluationTick = now - requestedTick <= value.settings().network().forgingLatencyCompensationTicks()
            ? requestedTick : now;
        ForgingGaugeCalculator.GaugeSnapshot gauge;
        try {
            gauge = BlacksmithOperationSessionRegistry.currentGauge(value.binding().session(), evaluationTick);
        } catch (RuntimeException exception) {
            feedback(player, ForgingFeedbackPacket.Status.ERROR);
            return;
        }
        ForgingGameService.StrikeResult result = ForgingGameService.acceptStrike(
            value.binding().table(), player, gauge.value()
        );
        if (result == ForgingGameService.StrikeResult.REJECTED) return;
        value.binding().session().acceptSequence(sequence);
        if (result == ForgingGameService.StrikeResult.FAILED) {
            feedback(player, ForgingFeedbackPacket.Status.FAILED);
            BlacksmithOperationSessionRegistry.release(value.binding().table(), false);
            player.closeContainer();
        } else if (result == ForgingGameService.StrikeResult.ACCEPTED) {
            feedbackForAcceptedStrike(player, value.binding(), now);
            sync(player, value.binding().session());
        } else {
            feedback(player, ForgingFeedbackPacket.Status.ERROR);
        }
    }

    public static void complete(ServerPlayer player, UUID sessionId, long sequence) {
        Optional<Validated> validated = validate(player, sessionId, sequence);
        if (validated.isEmpty()) return;
        Validated value = validated.get();
        ForgingGameService.CompletionResult result = ForgingGameService.complete(value.binding().table(), player);
        if (result == ForgingGameService.CompletionResult.REJECTED) return;
        value.binding().session().acceptSequence(sequence);
        if (result == ForgingGameService.CompletionResult.COMPLETED) {
            feedback(player, ForgingFeedbackPacket.Status.COMPLETED);
            BlacksmithOperationSessionRegistry.release(value.binding().table(), false);
            player.closeContainer();
        } else {
            feedback(player, ForgingFeedbackPacket.Status.ERROR);
        }
    }

    public static void pause(ServerPlayer player, UUID sessionId, long sequence, long requestedTick) {
        Optional<Validated> validated = validate(player, sessionId, sequence);
        if (validated.isEmpty()) return;
        Validated value = validated.get();
        long now = player.serverLevel().getGameTime();
        if (requestedTick > now || requestedTick < value.binding().session().baseServerTick()) return;
        long evaluationTick = now - requestedTick <= value.settings().network().forgingLatencyCompensationTicks()
            ? requestedTick : now;
        ForgingGaugeCalculator.GaugeSnapshot gauge =
            BlacksmithOperationSessionRegistry.currentGauge(value.binding().session(), evaluationTick);
        if (!ForgingGameService.pause(value.binding().table(), gauge.value(), gauge.direction())) return;
        value.binding().session().acceptSequence(sequence);
        BlacksmithOperationSessionRegistry.release(value.binding().table(), false);
    }

    public static void heartbeat(ServerPlayer player, UUID sessionId, long sequence) {
        Optional<Validated> validated = validate(player, sessionId, sequence);
        if (validated.isEmpty()) return;
        Validated value = validated.get();
        value.binding().session().acceptSequence(sequence);
        value.binding().session().heartbeat(player.serverLevel().getGameTime());
        BlacksmithSkillAssistResolver.resolveDisplay(player).ifPresent(snapshot -> {
            if (!snapshot.equals(value.binding().session().assistSnapshot())) {
                value.binding().session().updateAssistSnapshot(snapshot);
                sync(player, value.binding().session());
            }
        });
    }

    public static void sync(ServerPlayer player, ForgingSessionState session) {
        long now = player.serverLevel().getGameTime();
        ForgingGaugeCalculator.GaugeSnapshot gauge =
            BlacksmithOperationSessionRegistry.currentGauge(session, now);
        CraftboundNetwork.sendToPlayer(player, new ForgingSessionSyncPacket(
            session.sessionId(), session.lastSequence(), now,
            gauge.value(), gauge.direction(), session.assistSnapshot()
        ));
    }

    private static void feedbackForAcceptedStrike(ServerPlayer player, SessionBinding binding, long now) {
        var rough = RoughMetalPartStateService.read(binding.table().getWorkingPart());
        var progress = ForgingProgressStateService.read(binding.table().getWorkingPart());
        if (rough.isEmpty() || progress.isEmpty()) {
            feedback(player, ForgingFeedbackPacket.Status.STRIKE_ACCEPTED);
            return;
        }
        int remaining = rough.get().effectiveBreakOnHit() - progress.get().strikeHistory().size();
        if (remaining <= 2) {
            feedback(player, ForgingFeedbackPacket.Status.DANGER);
            return;
        }
        int threshold = BlacksmithSkillAssistResolver.resolveSmithingInstinctThreshold(player).orElse(0);
        int cooldown = BlacksmithSkillAssistDefinitions.INSTANCE.get()
            .map(value -> value.instinctAudio().cooldownTicks()).orElse(Integer.MAX_VALUE);
        if (threshold > 0 && remaining <= threshold
            && (binding.session().lastInstinctWarningTick() == Long.MIN_VALUE
                || now - binding.session().lastInstinctWarningTick() >= cooldown)) {
            binding.session().markInstinctWarning(now);
            feedback(player, ForgingFeedbackPacket.Status.INSTINCT);
        } else {
            feedback(player, ForgingFeedbackPacket.Status.STRIKE_ACCEPTED);
        }
    }

    private static Optional<Validated> validate(ServerPlayer player, UUID sessionId, long sequence) {
        if (!(player.containerMenu instanceof ForgingMenu menu)
            || !menu.stillValid(player)) return Optional.empty();
        Optional<SessionBinding> binding = BlacksmithOperationSessionRegistry.resolve(player, sessionId);
        Optional<BlacksmithSettings> settings = BlacksmithSettingsDefinitions.INSTANCE.get();
        if (binding.isEmpty() || settings.isEmpty()
            || menu.getForgingTable().orElse(null) != binding.get().table()
            || sequence <= binding.get().session().lastSequence()) return Optional.empty();
        return Optional.of(new Validated(binding.get(), settings.get()));
    }

    private static void feedback(ServerPlayer player, ForgingFeedbackPacket.Status status) {
        CraftboundNetwork.sendToPlayer(player, new ForgingFeedbackPacket(status));
    }

    private record Validated(SessionBinding binding, BlacksmithSettings settings) {}
}
