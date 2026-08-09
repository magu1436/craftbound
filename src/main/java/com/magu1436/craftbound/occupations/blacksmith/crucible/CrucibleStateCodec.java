package com.magu1436.craftbound.occupations.blacksmith.crucible;

import java.util.Optional;

import com.mojang.logging.LogUtils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import org.slf4j.Logger;

public final class CrucibleStateCodec {
    public static final String ROOT_KEY = "CraftboundCrucible";
    public static final String VERSION_KEY = "Version";
    public static final String METAL_KEY = "Metal";
    public static final String AMOUNT_KEY = "Amount";
    public static final String HEATING_TICKS_KEY = "HeatingTicks";
    public static final String PROCESS_STATE_KEY = "ProcessState";

    private static final Logger LOGGER = LogUtils.getLogger();

    private CrucibleStateCodec() {
    }

    public static Optional<CrucibleState> read(ItemStack stack) {
        return read(stack.getTag());
    }

    static Optional<CrucibleState> read(CompoundTag root) {
        if (root == null || !root.contains(ROOT_KEY)) {
            return Optional.of(CrucibleState.empty());
        }
        if (!root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return invalid("root value is not a compound tag");
        }

        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (!tag.contains(VERSION_KEY, Tag.TAG_INT)
            || !tag.contains(METAL_KEY, Tag.TAG_STRING)
            || !tag.contains(AMOUNT_KEY, Tag.TAG_INT)
            || !tag.contains(HEATING_TICKS_KEY, Tag.TAG_LONG)
            || !tag.contains(PROCESS_STATE_KEY, Tag.TAG_STRING)) {
            return invalid("one or more required fields are missing or mistyped");
        }

        int version = tag.getInt(VERSION_KEY);
        if (version != CrucibleState.CURRENT_VERSION) {
            return invalid("unsupported version " + version);
        }

        ResourceLocation metalId = ResourceLocation.tryParse(
            tag.getString(METAL_KEY)
        );
        if (metalId == null) {
            return invalid("invalid metal id");
        }

        Optional<CrucibleProcessState> processState =
            CrucibleProcessState.fromSerializedName(
                tag.getString(PROCESS_STATE_KEY)
            );
        if (processState.isEmpty()) {
            return invalid("unknown process state");
        }

        try {
            return Optional.of(new CrucibleState(
                version,
                metalId,
                tag.getInt(AMOUNT_KEY),
                tag.getLong(HEATING_TICKS_KEY),
                processState.get()
            ));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
    }

    public static void write(ItemStack stack, CrucibleState state) {
        if (state.processState() == CrucibleProcessState.EMPTY) {
            remove(stack);
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, state.version());
        tag.putString(METAL_KEY, state.metalId().toString());
        tag.putInt(AMOUNT_KEY, state.amount());
        tag.putLong(HEATING_TICKS_KEY, state.heatingTicks());
        tag.putString(PROCESS_STATE_KEY, state.processState().serializedName());
        stack.getOrCreateTag().put(ROOT_KEY, tag);
    }

    static void write(CompoundTag root, CrucibleState state) {
        if (state.processState() == CrucibleProcessState.EMPTY) {
            root.remove(ROOT_KEY);
            return;
        }

        CompoundTag tag = new CompoundTag();
        tag.putInt(VERSION_KEY, state.version());
        tag.putString(METAL_KEY, state.metalId().toString());
        tag.putInt(AMOUNT_KEY, state.amount());
        tag.putLong(HEATING_TICKS_KEY, state.heatingTicks());
        tag.putString(PROCESS_STATE_KEY, state.processState().serializedName());
        root.put(ROOT_KEY, tag);
    }

    public static void remove(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) {
            return;
        }
        root.remove(ROOT_KEY);
        if (root.isEmpty()) {
            stack.setTag(null);
        }
    }

    private static Optional<CrucibleState> invalid(String reason) {
        LOGGER.warn("Invalid crucible state data: {}", reason);
        return Optional.empty();
    }
}
