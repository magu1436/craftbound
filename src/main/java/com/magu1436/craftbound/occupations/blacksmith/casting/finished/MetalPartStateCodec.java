package com.magu1436.craftbound.occupations.blacksmith.casting.finished;

import java.util.Optional;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualDataCodec;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class MetalPartStateCodec {
    public static final String ROOT_KEY = "CraftboundMetalPart";
    private static final Logger LOGGER = LogUtils.getLogger();

    private MetalPartStateCodec() {}

    public static Optional<MetalPartState> read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (!tag.contains("Version", Tag.TAG_INT) || !tag.contains("Metal", Tag.TAG_STRING)
            || !tag.contains("Visual", Tag.TAG_COMPOUND)) {
            return invalid("missing or mistyped field");
        }
        String metalValue = tag.getString("Metal");
        ResourceLocation metalId = ResourceLocation.tryParse(metalValue);
        if (metalId == null || !metalValue.contains(":")) return invalid("invalid metal id");
        Optional<MetalVisualData> visual = MetalVisualDataCodec.read(tag.getCompound("Visual"));
        if (visual.isEmpty()) return invalid("invalid visual data");
        try {
            return Optional.of(new MetalPartState(tag.getInt("Version"), metalId, visual.get()));
        } catch (IllegalArgumentException exception) {
            return invalid(exception.getMessage());
        }
    }

    public static void write(ItemStack stack, MetalPartState state) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", state.version());
        tag.putString("Metal", state.metalId().toString());
        tag.put("Visual", MetalVisualDataCodec.write(state.visualData()));
        stack.getOrCreateTag().put(ROOT_KEY, tag);
    }

    private static Optional<MetalPartState> invalid(String reason) {
        LOGGER.warn("Invalid finished metal part state data: {}", reason);
        return Optional.empty();
    }
}
