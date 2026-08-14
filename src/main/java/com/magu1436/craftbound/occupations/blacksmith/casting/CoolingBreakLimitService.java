package com.magu1436.craftbound.occupations.blacksmith.casting;

import java.util.OptionalInt;

import com.magu1436.craftbound.common.CraftboundUtilities;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.CoolingDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;

import net.minecraft.resources.ResourceLocation;

public final class CoolingBreakLimitService {
    private static final ResourceLocation LINEAR_COOLING_BREAK_LIMIT =
        CraftboundUtilities.createResourceLocation("linear_cooling_break_limit");

    private CoolingBreakLimitService() {
    }

    public static OptionalInt evaluate(
        MetalPartDefinitionSnapshot snapshot,
        long coolingTicks
    ) {
        if (snapshot == null || coolingTicks < 0L) {
            return OptionalInt.empty();
        }

        CoolingDefinition cooling = snapshot.cooling();
        int breakOnHit = snapshot.forging().breakOnHit();
        if (!LINEAR_COOLING_BREAK_LIMIT.equals(cooling.evaluatorId())
            || cooling.minimumBreakOnHit() < 1
            || breakOnHit < cooling.minimumBreakOnHit()) {
            return OptionalInt.empty();
        }
        if (cooling.safeTicks() <= 0L) {
            return OptionalInt.of(breakOnHit);
        }

        double rate = Math.min(1.0D,
            (double) coolingTicks / (double) cooling.safeTicks());
        int effectiveBreakOnHit = Math.max(
            cooling.minimumBreakOnHit(),
            (int) Math.floor(breakOnHit * rate)
        );
        return OptionalInt.of(Math.min(effectiveBreakOnHit, breakOnHit));
    }
}
