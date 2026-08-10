package com.magu1436.craftbound.occupations.blacksmith.data;

import com.magu1436.craftbound.occupations.blacksmith.capability.IBlacksmithData;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinition.PrecisionScaleLevel;
import com.magu1436.craftbound.occupations.blacksmith.data.BlacksmithSkillAssistDefinition.SmithingInstinctLevel;
import com.magu1436.craftbound.occupations.blacksmith.forging.session.ForgingAssistSnapshot;
import com.magu1436.craftbound.registry.CraftboundCapabilities;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class BlacksmithSkillAssistResolver {
    private BlacksmithSkillAssistResolver() {}

    public static Optional<ForgingAssistSnapshot> resolveDisplay(ServerPlayer player) {
        Optional<BlacksmithSkillAssistDefinition> definition = BlacksmithSkillAssistDefinitions.INSTANCE.get();
        Optional<IBlacksmithData> data = player.getCapability(
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA
        ).resolve();
        if (definition.isEmpty() || data.isEmpty()) return Optional.empty();

        BlacksmithSkillAssistDefinition value = definition.get();
        int interval = value.forgingGauge().baseMarkInterval();
        for (PrecisionScaleLevel level : value.precisionScale()) {
            if (level.level() <= data.get().getPrecisionScaleLevel()) interval = level.markInterval();
        }
        return Optional.of(new ForgingAssistSnapshot(
            interval,
            data.get().getForceReadingLevel() >= value.forceReadingEnabledAtLevel()
                && value.showCurrentValue(),
            data.get().getStrikeReferenceLevel() >= value.strikeReferenceEnabledAtLevel()
                && value.strikeReferenceEnabled()
        ));
    }

    public static Optional<Integer> resolveSmithingInstinctThreshold(ServerPlayer player) {
        Optional<BlacksmithSkillAssistDefinition> definition = BlacksmithSkillAssistDefinitions.INSTANCE.get();
        Optional<IBlacksmithData> data = player.getCapability(
            CraftboundCapabilities.BLACKSMITH_CAPABILITY_DATA
        ).resolve();
        if (definition.isEmpty() || data.isEmpty()) return Optional.empty();
        int threshold = 0;
        for (SmithingInstinctLevel level : definition.get().smithingInstinct()) {
            if (level.level() <= data.get().getSmithingInstinctLevel()) threshold = level.forgingRemainingHits();
        }
        return threshold > 0 ? Optional.of(threshold) : Optional.empty();
    }
}
