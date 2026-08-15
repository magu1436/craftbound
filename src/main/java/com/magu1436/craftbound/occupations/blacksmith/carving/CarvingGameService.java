package com.magu1436.craftbound.occupations.blacksmith.carving;

import com.magu1436.craftbound.common.quality.QualityStateService;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.evaluation.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.logic.*;
import com.magu1436.craftbound.occupations.blacksmith.carving.state.CarvingProgressState;
import com.magu1436.craftbound.occupations.blacksmith.data.*;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

public final class CarvingGameService {
    private static final ResourceLocation IOU = new ResourceLocation("craftbound", "iou");
    private static final ResourceLocation REMAINING_RATIO = new ResourceLocation("craftbound", "remaining_ratio");
    private static final CarvingStrokeProcessor STROKES = new CarvingStrokeProcessor();
    private CarvingGameService() {}

    public static boolean canInsert(ItemStack stack) {
        return !NonMetalPartDefinitions.INSTANCE.matching(stack).isEmpty();
    }
    public static Optional<CarvingProgressState> start(CarvingTableBlockEntity table,
        ServerPlayer player, ResourceLocation partId) {
        NonMetalPartDefinition part = NonMetalPartDefinitions.INSTANCE.get(partId).orElse(null);
        if (part == null || !part.matches(table.material())) return Optional.empty();
        NonMetalMaterialDefinition material = NonMetalMaterialDefinitions.INSTANCE.get(part.materialProfileId()).orElse(null);
        CarvingShapeDefinition shape = CarvingShapeDefinitions.INSTANCE.get(part.shapeId()).orElse(null);
        if (material == null || shape == null || !supported(part)) return Optional.empty();
        ResourceLocation heldTool = ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem());
        if (!material.toolItemId().equals(heldTool)) return Optional.empty();
        BlacksmithSkillAssistDefinition.PrecisionShaping shaping = BlacksmithSkillAssistResolver
            .resolvePrecisionShaping(player).orElse(null);
        if (shaping == null) return Optional.empty();
        int gridSize = shaping.baseGridSize();
        CarvingDefinitionSnapshot snapshot = new CarvingDefinitionSnapshot(part.outputItemId(),
            part.ingredientCount(), material.toolItemId(),
            material.removePerPass(), material.pathInterpolation(), material.removedUnitsPerDurability(),
            part.warningRetention(), part.breakEvaluatorId(), part.breakThreshold(), part.shapeEvaluatorId(), shape.mask());
        CarvingProgressState progress = new CarvingProgressState(CarvingProgressState.CURRENT_VERSION,
            UUID.randomUUID(), part.id(), material.id(), gridSize, shaping.baseBrushRadius(),
            new CarvingGrid(gridSize), 0.0D, snapshot);
        return table.start(progress) ? Optional.of(progress) : Optional.empty();
    }

    public static StrokeResult applyStroke(CarvingTableBlockEntity table, ServerPlayer player, CarvingStroke stroke) {
        CarvingProgressState progress = table.progress().orElse(null);
        if (progress == null || !matchesTool(player.getMainHandItem(), progress.definitionSnapshot().requiredToolItemId()))
            return StrokeResult.REJECTED;
        CarvingGrid grid = progress.carvingGrid().copy();
        CarvingStrokeResult result;
        try { result = STROKES.apply(grid, stroke, progress.brushRadius(), progress.definitionSnapshot().removePerPass()); }
        catch (RuntimeException exception) { return StrokeResult.REJECTED; }
        double remainder = progress.removedUnitsRemainder() + result.actualRemovedUnits();
        int damageRequests = (int) Math.floor(remainder / progress.definitionSnapshot().removedUnitsPerDurability());
        remainder -= damageRequests * progress.definitionSnapshot().removedUnitsPerDurability();
        CarvingProgressState updated = new CarvingProgressState(progress.dataVersion(), progress.processId(),
            progress.partDefinitionId(), progress.materialProfileId(), progress.gridSize(), progress.brushRadius(),
            grid, remainder, progress.definitionSnapshot());
        if (new RemainingRatioBreakEvaluator().shouldBreak(grid, progress.definitionSnapshot().idealShape(),
            progress.definitionSnapshot().breakThreshold())) {
            CarvingExperienceResult experienceResult = new CarvingExperienceResult(
                progress.processId(), player.getUUID(), false, true);
            if (!table.clearBroken()) return StrokeResult.ERROR;
            CarvingExperienceHook.onResult(player, experienceResult);
            player.closeContainer(); return StrokeResult.BROKEN;
        }
        if (!table.updateProgress(updated)) return StrokeResult.ERROR;
        if (damageTool(player, damageRequests)) { player.closeContainer(); return StrokeResult.TOOL_BROKEN; }
        return result.changedCells().isEmpty() ? StrokeResult.UNCHANGED : StrokeResult.ACCEPTED;
    }

    public static FinalizeResult finalizeCarving(CarvingTableBlockEntity table, ServerPlayer operator) {
        return finalizeCarving(table, operator, true, true);
    }

    public static FinalizeResult finalizeCarvingWithoutExperience(CarvingTableBlockEntity table) {
        return finalizeCarving(table, null, false, false);
    }

    private static FinalizeResult finalizeCarving(CarvingTableBlockEntity table, ServerPlayer operator,
        boolean deliverToOperator, boolean grantExperience) {
        if (!table.reserveCompletion()) return FinalizeResult.REJECTED;
        CarvingProgressState progress = table.progress().orElse(null);
        if (progress == null) { table.cancelCompletion(); return FinalizeResult.UNSELECTED; }
        Item outputItem = ForgeRegistries.ITEMS.getValue(progress.definitionSnapshot().outputItemId());
        if (outputItem == null || outputItem == Items.AIR) { table.cancelCompletion(); return FinalizeResult.ERROR; }
        int quality = new IouCarvingShapeEvaluator().evaluate(progress.carvingGrid(), progress.definitionSnapshot().idealShape());
        ItemStack output = new ItemStack(outputItem);
        if (!QualityStateService.setQuality(output, quality) || !table.commitOutput(output)) {
            table.cancelCompletion(); return FinalizeResult.ERROR;
        }
        if (grantExperience && operator != null) {
            CarvingExperienceHook.onResult(operator, new CarvingExperienceResult(progress.processId(),
                operator.getUUID(), true, false));
        }
        if (deliverToOperator && operator != null) table.collect(operator);
        return FinalizeResult.COMPLETED;
    }

    private static boolean supported(NonMetalPartDefinition part) {
        return IOU.equals(part.shapeEvaluatorId()) && REMAINING_RATIO.equals(part.breakEvaluatorId());
    }
    private static boolean matchesTool(ItemStack stack, ResourceLocation id) {
        return !stack.isEmpty() && id.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }
    private static boolean damageTool(ServerPlayer player, int requests) {
        ItemStack tool = player.getMainHandItem();
        double preservation = BlacksmithSkillAssistResolver.resolveToolPreservationChance(player);
        for (int i = 0; i < requests && !tool.isEmpty(); i++) {
            int previousDamage = tool.getDamageValue();
            boolean broke = tool.hurt(1, player.getRandom(), player);
            if (tool.getDamageValue() != previousDamage && player.getRandom().nextDouble() < preservation) {
                tool.setDamageValue(previousDamage);
            } else if (broke) {
                player.broadcastBreakEvent(InteractionHand.MAIN_HAND);
                tool.shrink(1);
                tool.setDamageValue(0);
            }
        }
        return tool.isEmpty();
    }
    public enum StrokeResult { REJECTED, UNCHANGED, ACCEPTED, BROKEN, TOOL_BROKEN, ERROR }
    public enum FinalizeResult { REJECTED, UNSELECTED, COMPLETED, ERROR }
}
