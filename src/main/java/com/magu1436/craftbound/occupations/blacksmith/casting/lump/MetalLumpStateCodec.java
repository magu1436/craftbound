package com.magu1436.craftbound.occupations.blacksmith.casting.lump;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class MetalLumpStateCodec {
    public static final String ROOT_KEY = "CraftboundMetalLump";
    private MetalLumpStateCodec() {}
    public static Optional<MetalLumpState> read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag t = root.getCompound(ROOT_KEY);
        if (!t.contains("Version", Tag.TAG_INT) || !t.contains("Metal", Tag.TAG_STRING)
            || !t.contains("UnitsPerItem", Tag.TAG_INT)) return Optional.empty();
        ResourceLocation metal = ResourceLocation.tryParse(t.getString("Metal"));
        if (metal == null || !t.getString("Metal").contains(":")) return Optional.empty();
        try { return Optional.of(new MetalLumpState(t.getInt("Version"), metal, t.getInt("UnitsPerItem"))); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
    public static void write(ItemStack stack, MetalLumpState state) {
        CompoundTag t = new CompoundTag(); t.putInt("Version", state.version());
        t.putString("Metal", state.metalId().toString()); t.putInt("UnitsPerItem", state.unitsPerItem());
        stack.getOrCreateTag().put(ROOT_KEY, t);
    }
}
