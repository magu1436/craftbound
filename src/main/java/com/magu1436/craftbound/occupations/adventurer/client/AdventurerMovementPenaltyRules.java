package com.magu1436.craftbound.occupations.adventurer.client;

import java.util.List;

import com.magu1436.craftbound.event.MovementPenaltyReductionRule;
import com.magu1436.craftbound.registry.CraftboundAttributes;
import com.magu1436.craftbound.registry.CraftboundItemTags;

import net.minecraft.world.item.UseAnim;
import net.minecraftforge.common.ToolActions;

public final class AdventurerMovementPenaltyRules {

    private static final double SHIELD_FOOTWORK_REDUCTION_RATE = 0.1D;
    private static final double RANGED_FOOTWORK_REDUCTION_RATE = 0.1D;
    private static final double FIELD_RESUPPLY_REDUCTION_RATE = 0.1D;

    private AdventurerMovementPenaltyRules() {
    }

    public static List<MovementPenaltyReductionRule> create() {
        return List.of(
            new MovementPenaltyReductionRule(
                CraftboundAttributes.SHIELD_FOOTWORK::get,
                CraftboundItemTags.SHIELD_FOOTWORK_ITEMS,
                CraftboundItemTags.SHIELD_FOOTWORK_EXCLUDED_ITEMS,
                itemStack -> itemStack.canPerformAction(
                    ToolActions.SHIELD_BLOCK
                ),
                SHIELD_FOOTWORK_REDUCTION_RATE
            ),
            new MovementPenaltyReductionRule(
                CraftboundAttributes.RANGED_FOOTWORK::get,
                CraftboundItemTags.RANGED_FOOTWORK_ITEMS,
                CraftboundItemTags.RANGED_FOOTWORK_EXCLUDED_ITEMS,
                itemStack -> {
                    UseAnim useAnimation = itemStack.getUseAnimation();
                    return useAnimation == UseAnim.BOW
                        || useAnimation == UseAnim.CROSSBOW
                        || useAnimation == UseAnim.SPEAR;
                },
                RANGED_FOOTWORK_REDUCTION_RATE
            ),
            new MovementPenaltyReductionRule(
                CraftboundAttributes.FIELD_RESUPPLY::get,
                CraftboundItemTags.FIELD_RESUPPLY_ITEMS,
                CraftboundItemTags.FIELD_RESUPPLY_EXCLUDED_ITEMS,
                itemStack -> {
                    UseAnim useAnimation = itemStack.getUseAnimation();
                    return useAnimation == UseAnim.EAT
                        || useAnimation == UseAnim.DRINK;
                },
                FIELD_RESUPPLY_REDUCTION_RATE
            )
        );
    }
}
