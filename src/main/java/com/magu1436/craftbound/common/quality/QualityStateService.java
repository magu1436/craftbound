package com.magu1436.craftbound.common.quality;

import java.util.Optional;
import net.minecraft.world.item.ItemStack;

public final class QualityStateService {
    private QualityStateService() {}

    public static Optional<QualityState> read(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        return QualityStateCodec.read(stack);
    }

    public static boolean setQuality(ItemStack stack, int quality) {
        if (stack.isEmpty() || !QualityState.isValidQuality(quality)) return false;

        QualityStateCodec.write(stack, new QualityState(QualityState.CURRENT_VERSION, quality));
        return true;
    }

    public static boolean copyQuality(ItemStack source, ItemStack target) {
        if (target.isEmpty()) return false;

        Optional<QualityState> sourceState = read(source);
        if (sourceState.isEmpty()) return false;
        return setQuality(target, sourceState.get().quality());
    }
}
