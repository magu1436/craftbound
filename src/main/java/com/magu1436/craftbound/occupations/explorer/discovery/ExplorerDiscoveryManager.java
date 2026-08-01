package com.magu1436.craftbound.occupations.explorer.discovery;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;
import com.magu1436.craftbound.occupations.explorer.integration.ExplorerExperienceGateway.ExperienceGrantResult;
import com.magu1436.craftbound.occupations.explorer.integration.PufferfishExplorerExperienceGateway;
import com.magu1436.craftbound.occupations.explorer.registry.ExplorerDiscoveryRegistry;
import com.magu1436.craftbound.registry.CraftboundCapabilities;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/** 発見スキャン、経験値付与、履歴保存、通知を統括する。 */
@Mod.EventBusSubscriber(
    modid = Craftbound.MODID,
    bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class ExplorerDiscoveryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long ERROR_LOG_INTERVAL_TICKS = 20L * 60L;
    private static final Comparator<DiscoveryTarget> DISCOVERY_ORDER =
        Comparator.comparingInt(ExplorerDiscoveryManager::orderOf)
            .thenComparing(DiscoveryTarget::candidateKey);
    private static final ExplorerDiscoveryManager INSTANCE = create();

    private final ExplorerDiscoveryScanner scanner;
    private final ExplorerCandidateTracker candidateTracker;
    private final PufferfishExplorerExperienceGateway experienceGateway;
    private final Map<String, Long> lastErrorLogTimes = new HashMap<>();

    private ExplorerDiscoveryManager(
        ExplorerDiscoveryScanner scanner,
        ExplorerCandidateTracker candidateTracker,
        PufferfishExplorerExperienceGateway experienceGateway
    ) {
        this.scanner = scanner;
        this.candidateTracker = candidateTracker;
        this.experienceGateway = experienceGateway;
    }

    private static ExplorerDiscoveryManager create() {
        ExplorerDiscoveryRegistry registry =
            ExplorerDiscoveryRegistry.INSTANCE;
        ExplorerCandidateTracker tracker = new ExplorerCandidateTracker();
        ExplorerDiscoveryManager manager = new ExplorerDiscoveryManager(
            new ExplorerDiscoveryScanner(
                new BiomeDiscoveryDetector(registry),
                new DimensionDiscoveryDetector(registry)
            ),
            tracker,
            new PufferfishExplorerExperienceGateway()
        );
        registry.setReloadCompletedCallback(manager::onDataPackReload);
        return manager;
    }

    public void tick(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        if (!shouldScan(player, gameTime)) {
            return;
        }
        if (!isEligible(player)) {
            candidateTracker.clear(player.getUUID());
            return;
        }

        ExplorerDiscoveryScanner.DiscoveryScanResult scan =
            scanner.scan(player);
        List<DiscoveryTarget> ready = candidateTracker.update(
            player, scan, gameTime
        );
        ready.stream()
            .sorted(DISCOVERY_ORDER)
            .forEach(target -> completeDiscovery(player, target, gameTime));
    }

    public void clear(ServerPlayer player) {
        candidateTracker.clear(player.getUUID());
    }

    public void onDataPackReload() {
        candidateTracker.clearAll();
        lastErrorLogTimes.clear();
    }

    private void completeDiscovery(
        ServerPlayer player,
        DiscoveryTarget target,
        long gameTime
    ) {
        ExplorerDiscoveryData data = player.getCapability(
            CraftboundCapabilities.EXPLORER_DISCOVERY_DATA
        ).resolve().orElse(null);
        if (data == null) {
            logGrantFailure(
                player, target, "capability_unavailable", gameTime
            );
            return;
        }
        if (isAlreadyDiscovered(data, target)) {
            candidateTracker.markCompleted(player.getUUID(), target);
            return;
        }
        if (
            target instanceof DiscoveryTarget.Structure structure
                && hasReachedStructureLimit(data, structure)
        ) {
            candidateTracker.markCompleted(player.getUUID(), target);
            return;
        }

        if (experienceGateway.isAtMaximumLevel(player)) {
            recordDiscovery(data, target);
            notifyDiscovery(player, target, false);
            candidateTracker.markCompleted(player.getUUID(), target);
            return;
        }

        ExperienceGrantResult result = experienceGateway.grantExperience(
            player,
            PufferfishExplorerExperienceGateway.EXPERIENCE_SOURCE_ID,
            target.xp()
        );
        if (result != ExperienceGrantResult.SUCCESS) {
            logGrantFailure(player, target, result.name(), gameTime);
            return;
        }

        recordDiscovery(data, target);
        notifyDiscovery(player, target, true);
        candidateTracker.markCompleted(player.getUUID(), target);
    }

    private static boolean shouldScan(ServerPlayer player, long gameTime) {
        int offset = Math.floorMod(player.getUUID().hashCode(), 20);
        return Math.floorMod(gameTime + offset, 20) == 0;
    }

    private static boolean isEligible(ServerPlayer player) {
        return !(player instanceof FakePlayer)
            && player.isAlive()
            && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
    }

    private static boolean isAlreadyDiscovered(
        ExplorerDiscoveryData data,
        DiscoveryTarget target
    ) {
        if (target instanceof DiscoveryTarget.Biome biome) {
            return data.hasDiscoveredBiome(biome.targetId().toString());
        }
        if (target instanceof DiscoveryTarget.Dimension dimension) {
            return data.hasDiscoveredDimension(
                dimension.targetId().toString()
            );
        }
        DiscoveryTarget.Structure structure =
            (DiscoveryTarget.Structure) target;
        return data.hasDiscoveredStructure(
            structure.targetId().toString(),
            structure.instanceKey()
        );
    }

    private static void recordDiscovery(
        ExplorerDiscoveryData data,
        DiscoveryTarget target
    ) {
        if (target instanceof DiscoveryTarget.Biome biome) {
            data.recordBiome(biome.targetId().toString());
        } else if (target instanceof DiscoveryTarget.Dimension dimension) {
            data.recordDimension(dimension.targetId().toString());
        } else {
            DiscoveryTarget.Structure structure =
                (DiscoveryTarget.Structure) target;
            data.recordStructure(
                structure.targetId().toString(),
                structure.instanceKey()
            );
        }
    }

    private static boolean hasReachedStructureLimit(
        ExplorerDiscoveryData data,
        DiscoveryTarget.Structure target
    ) {
        return target.maxDiscoveries() != -1
            && data.getStructureDiscoveryCount(target.targetId().toString())
                >= target.maxDiscoveries();
    }

    private static void notifyDiscovery(
        ServerPlayer player,
        DiscoveryTarget target,
        boolean includeExperience
    ) {
        Component displayName = displayName(target);
        String type = target.type().name().toLowerCase();
        String suffix = includeExperience
            ? "with_experience"
            : "without_experience";
        String key = "message.craftbound.explorer.discovery."
            + type + "." + suffix;
        Component message = includeExperience
            ? Component.translatableWithFallback(
                key,
                "[Exploration] Discovered %1$s (+%2$s XP)",
                displayName,
                target.xp()
            )
            : Component.translatableWithFallback(
                key,
                "[Exploration] Discovered %1$s",
                displayName
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
        String fallback = id.toString();
        if (configuredKey != null) {
            return Component.translatableWithFallback(
                configuredKey, fallback
            );
        }
        String prefix = target.type() == DiscoveryTarget.DiscoveryType.BIOME
            ? "biome."
            : target.type() == DiscoveryTarget.DiscoveryType.DIMENSION
                ? "dimension."
                : "structure.";
        return Component.translatableWithFallback(
            prefix + id.getNamespace() + "." + id.getPath(),
            fallback
        );
    }

    private void logGrantFailure(
        ServerPlayer player,
        DiscoveryTarget target,
        String reason,
        long gameTime
    ) {
        String key = player.getUUID() + "|" + target.candidateKey()
            + "|" + reason;
        Long previous = lastErrorLogTimes.get(key);
        if (
            previous != null
                && gameTime - previous < ERROR_LOG_INTERVAL_TICKS
        ) {
            return;
        }
        lastErrorLogTimes.put(key, gameTime);
        LOGGER.error(
            "Could not grant explorer discovery experience to {} for {}: {}",
            player.getUUID(),
            target.candidateKey(),
            reason
        );
    }

    private static int orderOf(DiscoveryTarget target) {
        return switch (target.type()) {
            case DIMENSION -> 0;
            case BIOME -> 1;
            case STRUCTURE -> 2;
        };
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (
            event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player
        ) {
            INSTANCE.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.clear(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(
        PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.clear(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.clear(player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            INSTANCE.clear(player);
        }
    }

    @SubscribeEvent
    public static void onGameModeChanged(
        PlayerEvent.PlayerChangeGameModeEvent event
    ) {
        if (
            event.getNewGameMode() != GameType.SURVIVAL
                && event.getEntity() instanceof ServerPlayer player
        ) {
            INSTANCE.clear(player);
        }
    }
}
