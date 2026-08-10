package com.magu1436.craftbound.common.quality;

import com.mojang.logging.LogUtils;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class QualityStateCodec {
    public static final String ROOT_KEY = "CraftboundQuality";

    private static final String VERSION_KEY = "Version";
    private static final String QUALITY_KEY = "Quality";
    private static final Logger LOGGER = LogUtils.getLogger();

    private QualityStateCodec() {}

    public static Optional<QualityState> read(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();

        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) return Optional.empty();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return invalid("quality root is not a compound");
        }

        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (!tag.contains(VERSION_KEY, Tag.TAG_INT)
            || !tag.contains(QUALITY_KEY, Tag.TAG_INT)) {
            return invalid("missing or mistyped field");
        }

        try {
            return Optional.of(new QualityState(
                tag.getInt(VERSION_KEY),
                tag.getInt(QUALITY_KEY)
            ));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
    }

    public static void write(ItemStack stack, QualityState state) {
        Objects.requireNonNull(state, "state");

        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, state.version());
        tag.putInt(QUALITY_KEY, state.quality());
        stack.getOrCreateTag().put(ROOT_KEY, tag);
    }

    private static Optional<QualityState> invalid(String reason) {
        LOGGER.warn("Invalid quality state data: {}", reason);
        return Optional.empty();
    }
}
