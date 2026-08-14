package com.magu1436.craftbound.occupations.blacksmith.forging.state;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class ForgingProgressStateCodec {
    public static final String ROOT_KEY = "CraftboundForgingProgress";

    private static final String VERSION_KEY = "Version";
    private static final String STRIKE_HISTORY_KEY = "StrikeHistory";
    private static final String GAUGE_VALUE_KEY = "GaugeValue";
    private static final String GAUGE_DIRECTION_KEY = "GaugeDirection";
    private static final Logger LOGGER = LogUtils.getLogger();

    private ForgingProgressStateCodec() {}

    public static boolean hasRoot(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.contains(ROOT_KEY);
    }

    public static Optional<ForgingProgressState> read(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY)) return Optional.empty();
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return invalid("progress root is not a compound");
        }

        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (!tag.contains(VERSION_KEY, Tag.TAG_INT)
            || !tag.contains(STRIKE_HISTORY_KEY, Tag.TAG_LIST)
            || !tag.contains(GAUGE_VALUE_KEY, Tag.TAG_DOUBLE)
            || !tag.contains(GAUGE_DIRECTION_KEY, Tag.TAG_STRING)) {
            return invalid("missing or mistyped field");
        }

        ListTag strikeTags = (ListTag) tag.get(STRIKE_HISTORY_KEY);
        if (!strikeTags.isEmpty() && strikeTags.getElementType() != Tag.TAG_DOUBLE) {
            return invalid("strike history is not a double list");
        }
        List<Double> strikeHistory = new ArrayList<>(strikeTags.size());
        for (Tag strikeTag : strikeTags) {
            strikeHistory.add(((DoubleTag) strikeTag).getAsDouble());
        }

        Optional<GaugeDirection> direction = GaugeDirection.fromSerializedName(
            tag.getString(GAUGE_DIRECTION_KEY)
        );
        if (direction.isEmpty()) return invalid("unknown gauge direction");

        try {
            return Optional.of(new ForgingProgressState(
                tag.getInt(VERSION_KEY),
                strikeHistory,
                tag.getDouble(GAUGE_VALUE_KEY),
                direction.get()
            ));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
    }

    public static void write(ItemStack stack, ForgingProgressState state) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, state.version());
        ListTag strikeTags = new ListTag();
        for (double strength : state.strikeHistory()) {
            strikeTags.add(DoubleTag.valueOf(strength));
        }
        tag.put(STRIKE_HISTORY_KEY, strikeTags);
        tag.putDouble(GAUGE_VALUE_KEY, state.gaugeValue());
        tag.putString(GAUGE_DIRECTION_KEY, state.gaugeDirection().serializedName());
        stack.getOrCreateTag().put(ROOT_KEY, tag);
    }

    private static Optional<ForgingProgressState> invalid(String reason) {
        LOGGER.warn("Invalid forging progress state data: {}", reason);
        return Optional.empty();
    }
}
