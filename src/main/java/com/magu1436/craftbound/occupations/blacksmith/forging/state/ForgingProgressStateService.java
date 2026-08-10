package com.magu1436.craftbound.occupations.blacksmith.forging.state;

import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartState;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class ForgingProgressStateService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgingProgressStateService() {}

    public static boolean hasStoredState(ItemStack stack) {
        return ForgingProgressStateCodec.hasRoot(stack);
    }

    public static boolean isUsable(ItemStack stack) {
        if (RoughMetalPartStateService.read(stack).isEmpty()) return false;
        return !hasStoredState(stack) || read(stack).isPresent();
    }

    public static Optional<ForgingProgressState> read(ItemStack stack) {
        Optional<RoughMetalPartState> roughState = RoughMetalPartStateService.read(stack);
        if (roughState.isEmpty()) return Optional.empty();

        Optional<ForgingProgressState> progress = ForgingProgressStateCodec.read(stack);
        if (progress.isEmpty()) return Optional.empty();
        if (progress.get().strikeHistory().size() >= roughState.get().effectiveBreakOnHit()) {
            LOGGER.warn("Forging progress has reached or exceeded its effective break-on-hit limit");
            return Optional.empty();
        }
        return progress;
    }

    public static boolean initialize(
        ItemStack stack,
        double gaugeValue,
        GaugeDirection direction
    ) {
        if (hasStoredState(stack) || RoughMetalPartStateService.read(stack).isEmpty()) return false;
        try {
            ForgingProgressStateCodec.write(stack, new ForgingProgressState(
                ForgingProgressState.CURRENT_VERSION,
                List.of(),
                gaugeValue,
                direction
            ));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean saveGauge(
        ItemStack stack,
        double gaugeValue,
        GaugeDirection direction
    ) {
        Optional<ForgingProgressState> current = read(stack);
        if (current.isEmpty()) return false;
        try {
            ForgingProgressState state = current.get();
            ForgingProgressStateCodec.write(stack, new ForgingProgressState(
                state.version(),
                state.strikeHistory(),
                gaugeValue,
                direction
            ));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static Optional<ForgingProgressState> appendStrike(ItemStack stack, double strength) {
        Optional<ForgingProgressState> current = read(stack);
        Optional<RoughMetalPartState> roughState = RoughMetalPartStateService.read(stack);
        if (current.isEmpty() || roughState.isEmpty() || !ForgingProgressState.isValidValue(strength)) {
            return Optional.empty();
        }

        List<Double> strikes = new ArrayList<>(current.get().strikeHistory());
        strikes.add(strength);
        if (strikes.size() >= roughState.get().effectiveBreakOnHit()) return Optional.empty();

        ForgingProgressState updated = new ForgingProgressState(
            current.get().version(),
            strikes,
            current.get().gaugeValue(),
            current.get().gaugeDirection()
        );
        ForgingProgressStateCodec.write(stack, updated);
        return Optional.of(updated);
    }

    public static boolean saveTerminalStrike(ItemStack stack, double strength) {
        Optional<ForgingProgressState> current = read(stack);
        Optional<RoughMetalPartState> roughState = RoughMetalPartStateService.read(stack);
        if (current.isEmpty() || roughState.isEmpty() || !ForgingProgressState.isValidValue(strength)) {
            return false;
        }
        List<Double> strikes = new ArrayList<>(current.get().strikeHistory());
        strikes.add(strength);
        if (strikes.size() != roughState.get().effectiveBreakOnHit()) return false;
        ForgingProgressStateCodec.write(stack, new ForgingProgressState(
            current.get().version(),
            strikes,
            current.get().gaugeValue(),
            current.get().gaugeDirection()
        ));
        return true;
    }
}
