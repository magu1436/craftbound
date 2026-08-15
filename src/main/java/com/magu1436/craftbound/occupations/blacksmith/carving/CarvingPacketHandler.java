package com.magu1436.craftbound.occupations.blacksmith.carving;

import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.network.packet.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.evaluation.RemainingRatioBreakEvaluator;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.magu1436.craftbound.occupations.blacksmith.carving.menu.CarvingMenu;
import com.magu1436.craftbound.occupations.blacksmith.carving.session.CarvingSessionState;
import com.magu1436.craftbound.occupations.blacksmith.data.*;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.BlacksmithOperationSessionRegistry.CarvingBinding;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class CarvingPacketHandler {
    private CarvingPacketHandler() {}
    public static void stroke(ServerPlayer player, CarvingStrokeRequestPacket packet) {
        Optional<Validated> validated = validate(player, packet.sessionId(), packet.sequence());
        if (validated.isEmpty() || !coordinates(packet)) return;
        CarvingBinding binding = validated.get().binding();
        var progress = binding.table().progress().orElse(null); if (progress == null) return;
        int size = progress.gridSize();
        CarvingStroke stroke = new CarvingStroke(cell(packet.startX(), size), cell(packet.startY(), size),
            cell(packet.endX(), size), cell(packet.endY(), size), packet.excludeStart());
        int pathCells = SupercoverLine.trace(stroke.startX(), stroke.startY(), stroke.endX(), stroke.endY()).size();
        int brushWidth = (int) Math.ceil(progress.brushRadius()) * 2 + 1;
        if ((long) pathCells * brushWidth * brushWidth > validated.get().settings().network().carvingMaxCellsPerRequest()) return;
        CarvingGrid before = progress.carvingGrid();
        CarvingGameService.StrokeResult result = CarvingGameService.applyStroke(binding.table(), player, stroke);
        if (result == CarvingGameService.StrokeResult.REJECTED || result == CarvingGameService.StrokeResult.ERROR) return;
        binding.session().acceptSequence(packet.sequence()); binding.session().heartbeat(player.serverLevel().getGameTime());
        CarvingGrid after = binding.table().progress().map(value -> value.carvingGrid()).orElse(before);
        CraftboundNetwork.sendToPlayer(player, new CarvingDeltaSyncPacket(binding.session().sessionId(),
            packet.sequence(), changes(before, after)));
        double retention = binding.table().progress().map(value -> new RemainingRatioBreakEvaluator()
            .retention(value.carvingGrid(), value.definitionSnapshot().idealShape())).orElse(1.0D);
        double warning = binding.table().progress().map(value -> value.definitionSnapshot().warningRetention()).orElse(0.0D);
        CarvingFeedbackPacket.Status feedback = switch (result) {
            case BROKEN -> CarvingFeedbackPacket.Status.BROKEN;
            case TOOL_BROKEN -> CarvingFeedbackPacket.Status.TOOL_BROKEN;
            default -> retention <= warning ? CarvingFeedbackPacket.Status.WARNING : CarvingFeedbackPacket.Status.ACCEPTED;
        };
        if (feedback == CarvingFeedbackPacket.Status.WARNING) player.serverLevel().playSound(null,
            binding.table().getBlockPos(), SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.25F, 0.65F);
        CraftboundNetwork.sendToPlayer(player, new CarvingFeedbackPacket(feedback));
        if (result == CarvingGameService.StrokeResult.BROKEN || result == CarvingGameService.StrokeResult.TOOL_BROKEN) {
            BlacksmithOperationSessionRegistry.release(binding.table()); player.closeContainer();
        }
    }
    public static void heartbeat(ServerPlayer player, CarvingHeartbeatPacket packet) {
        Optional<Validated> validated = validate(player, packet.sessionId(), packet.sequence());
        if (validated.isEmpty()) return;
        validated.get().binding().session().acceptSequence(packet.sequence());
        validated.get().binding().session().heartbeat(player.serverLevel().getGameTime());
        CraftboundNetwork.sendToPlayer(player, new CarvingDeltaSyncPacket(packet.sessionId(), packet.sequence(), List.of()));
    }
    public static void syncFull(ServerPlayer player, CarvingTableBlockEntity table, CarvingSessionState session) {
        table.progress().ifPresent(progress -> CraftboundNetwork.sendToPlayer(player, new CarvingSessionSyncPacket(
            session.sessionId(), session.lastSequence(), progress.brushRadius(),
            progress.definitionSnapshot().removePerPass(), progress.carvingGrid(), table.material(),
            new ItemStack(ForgeRegistries.ITEMS.getValue(progress.definitionSnapshot().outputItemId())),
            NonMetalMaterialDefinitions.INSTANCE.get(progress.materialProfileId())
                .map(value -> value.carvingTexture()).orElse(null))));
    }
    private static Optional<Validated> validate(ServerPlayer player, UUID sessionId, long sequence) {
        if (!(player.containerMenu instanceof CarvingMenu menu) || !menu.stillValid(player)) return Optional.empty();
        Optional<CarvingBinding> binding = BlacksmithOperationSessionRegistry.resolveCarving(player, sessionId);
        Optional<BlacksmithSettings> settings = BlacksmithSettingsDefinitions.INSTANCE.get();
        if (binding.isEmpty() || settings.isEmpty() || sequence <= binding.get().session().lastSequence()
            || menu.getCarvingTable().orElse(null) != binding.get().table()) return Optional.empty();
        return Optional.of(new Validated(binding.get(), settings.get()));
    }
    private static boolean coordinates(CarvingStrokeRequestPacket p) {
        return normalized(p.startX()) && normalized(p.startY()) && normalized(p.endX()) && normalized(p.endY());
    }
    private static boolean normalized(double value) { return Double.isFinite(value) && value >= 0.0D && value < 1.0D; }
    private static int cell(double normalized, int size) { return Math.min(size - 1, (int) (normalized * size)); }
    private static List<CarvingStrokeResult.ChangedCell> changes(CarvingGrid before, CarvingGrid after) {
        if (before.size() != after.size()) return List.of();
        List<CarvingStrokeResult.ChangedCell> result = new ArrayList<>();
        for (int y = 0; y < before.size(); y++) for (int x = 0; x < before.size(); x++)
            if (Double.compare(before.get(x, y), after.get(x, y)) != 0)
                result.add(new CarvingStrokeResult.ChangedCell(x, y, after.get(x, y)));
        return result;
    }
    private record Validated(CarvingBinding binding, BlacksmithSettings settings) {}
}
