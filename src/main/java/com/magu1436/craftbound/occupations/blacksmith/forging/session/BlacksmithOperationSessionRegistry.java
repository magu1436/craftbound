package com.magu1436.craftbound.occupations.blacksmith.forging.session;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettings;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSettingsDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinition;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistResolver;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingGameService;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingGaugeCalculator;
import com.magu1436.craftbound.occupations.blacksmith.forging.ForgingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.forging.menu.ForgingMenu;
import com.magu1436.craftbound.occupations.blacksmith.forging.state.ForgingProgressState;
import com.magu1436.craftbound.occupations.blacksmith.carving.CarvingTableBlockEntity;
import com.magu1436.craftbound.occupations.blacksmith.carving.menu.CarvingMenu;
import com.magu1436.craftbound.occupations.blacksmith.carving.session.CarvingSessionState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlacksmithOperationSessionRegistry {
    private static final Map<UUID, ForgingTableBlockEntity> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, CarvingTableBlockEntity> CARVING_BY_PLAYER = new HashMap<>();

    private BlacksmithOperationSessionRegistry() {}

    public static synchronized Optional<ForgingSessionState> acquire(
        ServerPlayer player,
        ForgingTableBlockEntity table
    ) {
        Optional<BlacksmithSettings> settings = BlacksmithSettingsDefinitions.INSTANCE.get();
        Optional<BlacksmithSkillAssistDefinition> assists = BlacksmithSkillAssistDefinitions.INSTANCE.get();
        Optional<ForgingAssistSnapshot> assistSnapshot = BlacksmithSkillAssistResolver.resolveDisplay(player);
        if (settings.isEmpty() || assists.isEmpty() || assistSnapshot.isEmpty()) return Optional.empty();

        ForgingSessionState tableSession = table.getActiveSession();
        if (tableSession != null) {
            if (!tableSession.activePlayerId().equals(player.getUUID())) return Optional.empty();
            BY_PLAYER.put(player.getUUID(), table);
            tableSession.heartbeat(player.serverLevel().getGameTime());
            return Optional.of(tableSession);
        }

        ForgingTableBlockEntity previous = BY_PLAYER.get(player.getUUID());
        if (previous != null && previous != table) releaseInternal(previous, true);

        BlacksmithSkillAssistDefinition.Gauge gauge = assists.get().forgingGauge();
        Optional<ForgingProgressState> progress = ForgingGameService.startOrResume(
            table, gauge.initialValue(), gauge.initialDirection()
        );
        if (progress.isEmpty()) return Optional.empty();

        long now = player.serverLevel().getGameTime();
        ForgingSessionState session = new ForgingSessionState(
            UUID.randomUUID(), player.getUUID(), now,
            progress.get().gaugeValue(), progress.get().gaugeDirection(), now, assistSnapshot.get()
        );
        if (!table.setActiveSession(session)) return Optional.empty();
        BY_PLAYER.put(player.getUUID(), table);
        return Optional.of(session);
    }

    public static synchronized Optional<SessionBinding> resolve(ServerPlayer player, UUID sessionId) {
        ForgingTableBlockEntity table = BY_PLAYER.get(player.getUUID());
        if (table == null) return Optional.empty();
        ForgingSessionState session = table.getActiveSession();
        if (session == null || !session.sessionId().equals(sessionId)
            || !session.activePlayerId().equals(player.getUUID())) return Optional.empty();
        return Optional.of(new SessionBinding(table, session));
    }

    public static synchronized Optional<CarvingSessionState> acquire(ServerPlayer player,
        CarvingTableBlockEntity table) {
        var progress = table.progress();
        if (progress.isEmpty()) return Optional.empty();
        CarvingSessionState current = table.activeSession();
        if (current != null) {
            if (!current.activePlayerId().equals(player.getUUID())) return Optional.empty();
            current.heartbeat(player.serverLevel().getGameTime());
            CARVING_BY_PLAYER.put(player.getUUID(), table);
            return Optional.of(current);
        }
        CarvingTableBlockEntity previous = CARVING_BY_PLAYER.get(player.getUUID());
        if (previous != null && previous != table) release(previous);
        CarvingSessionState session = new CarvingSessionState(UUID.randomUUID(), player.getUUID(),
            progress.get().processId(), player.serverLevel().getGameTime());
        if (!table.setActiveSession(session)) return Optional.empty();
        CARVING_BY_PLAYER.put(player.getUUID(), table);
        return Optional.of(session);
    }

    public static synchronized Optional<CarvingBinding> resolveCarving(ServerPlayer player, UUID sessionId) {
        CarvingTableBlockEntity table = CARVING_BY_PLAYER.get(player.getUUID());
        if (table == null || table.activeSession() == null) return Optional.empty();
        CarvingSessionState session = table.activeSession();
        return session.sessionId().equals(sessionId) && session.activePlayerId().equals(player.getUUID())
            ? Optional.of(new CarvingBinding(table, session)) : Optional.empty();
    }

    public static synchronized void release(CarvingTableBlockEntity table) {
        CarvingSessionState session = table.activeSession();
        if (session != null) table.clearActiveSession(session.sessionId());
        CARVING_BY_PLAYER.entrySet().removeIf(entry -> entry.getValue() == table);
    }

    public static synchronized void release(ForgingTableBlockEntity table, boolean pause) {
        releaseInternal(table, pause);
    }

    public static ForgingGaugeCalculator.GaugeSnapshot currentGauge(
        ForgingSessionState session,
        long tick
    ) {
        BlacksmithSkillAssistDefinition.Gauge gauge = BlacksmithSkillAssistDefinitions.INSTANCE.get()
            .orElseThrow().forgingGauge();
        return ForgingGaugeCalculator.valueAt(session.baseServerTick(), session.baseGaugeValue(),
            session.baseGaugeDirection(), tick, gauge.min(), gauge.max(), gauge.cycleTicks());
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        BlacksmithSettings settings = BlacksmithSettingsDefinitions.INSTANCE.get().orElse(null);
        if (settings == null) return;
        long now = event.getServer().overworld().getGameTime();
        synchronized (BlacksmithOperationSessionRegistry.class) {
            for (ForgingTableBlockEntity table : new ArrayList<>(BY_PLAYER.values())) {
                ForgingSessionState session = table.getActiveSession();
                if (session == null) continue;
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.activePlayerId());
                if (player == null) {
                    session.markDisconnected(now);
                    if (now - session.disconnectedAtTick() >= settings.network().disconnectGraceTicks()) {
                        releaseInternal(table, true);
                    }
                    continue;
                }
                if (!isValidOnlineLease(player, table, settings)) releaseInternal(table, true);
            }
            for (CarvingTableBlockEntity table : new ArrayList<>(CARVING_BY_PLAYER.values())) {
                CarvingSessionState session = table.activeSession();
                if (session == null) continue;
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(session.activePlayerId());
                if (player == null) {
                    session.markDisconnected(now);
                    if (now - session.disconnectedAtTick() >= settings.network().disconnectGraceTicks()) release(table);
                } else if (!player.isAlive() || player.level() != table.getLevel()
                    || !(player.containerMenu instanceof CarvingMenu menu)
                    || menu.getCarvingTable().orElse(null) != table
                    || now - session.lastHeartbeatTick() > settings.network().sessionHeartbeatTicks() * 2L
                    || player.distanceToSqr(table.getBlockPos().getCenter())
                        > Math.pow(settings.network().workbenchInteractionDistance(), 2.0D)) {
                    release(table);
                }
            }
        }
    }

    private static boolean isValidOnlineLease(ServerPlayer player, ForgingTableBlockEntity table,
        BlacksmithSettings settings) {
        if (!player.isAlive() || player.level() != table.getLevel()
            || !(player.containerMenu instanceof ForgingMenu menu)
            || menu.getForgingTable().orElse(null) != table) return false;
        double maxDistance = settings.network().workbenchInteractionDistance();
        return player.distanceToSqr(table.getBlockPos().getCenter()) <= maxDistance * maxDistance;
    }

    private static void releaseInternal(ForgingTableBlockEntity table, boolean pause) {
        ForgingSessionState session = table.getActiveSession();
        if (session == null) return;
        if (pause && table.getLevel() instanceof ServerLevel level) {
            try {
                ForgingGaugeCalculator.GaugeSnapshot gauge = currentGauge(session, level.getGameTime());
                ForgingGameService.pause(table, gauge.value(), gauge.direction());
            } catch (RuntimeException ignored) {
                // Invalid reload data must not destroy the stored working part.
            }
        }
        table.clearActiveSession(session.sessionId());
        BY_PLAYER.remove(session.activePlayerId(), table);
    }

    public record SessionBinding(ForgingTableBlockEntity table, ForgingSessionState session) {}
    public record CarvingBinding(CarvingTableBlockEntity table, CarvingSessionState session) {}
}
