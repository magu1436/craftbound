package com.magu1436.craftbound.occupations.blacksmith.material;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

public final class MetalVisualDataCodec {
    private MetalVisualDataCodec() {}

    public static CompoundTag write(MetalVisualData data) {
        CompoundTag tag = new CompoundTag();
        if (data.explicitRgb() != null) tag.putInt("ExplicitRgb", data.explicitRgb());
        if (data.representativeItemId() != null) {
            tag.putString("RepresentativeItem", data.representativeItemId().toString());
        }
        return tag;
    }

    public static Optional<MetalVisualData> read(CompoundTag tag) {
        if (tag.contains("ExplicitRgb") && !tag.contains("ExplicitRgb", Tag.TAG_INT)) return Optional.empty();
        if (tag.contains("RepresentativeItem") && !tag.contains("RepresentativeItem", Tag.TAG_STRING)) return Optional.empty();
        Integer color = tag.contains("ExplicitRgb", Tag.TAG_INT) ? tag.getInt("ExplicitRgb") : null;
        if (color != null && (color < 0 || color > 0xFFFFFF)) return Optional.empty();
        ResourceLocation itemId = null;
        if (tag.contains("RepresentativeItem", Tag.TAG_STRING)) {
            String value = tag.getString("RepresentativeItem");
            itemId = ResourceLocation.tryParse(value);
            if (itemId == null || !value.contains(":") || !ForgeRegistries.ITEMS.containsKey(itemId)) return Optional.empty();
        }
        if (color == null && itemId == null) return Optional.empty();
        return Optional.of(new MetalVisualData(color, itemId));
    }
}
