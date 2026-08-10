package com.magu1436.craftbound.occupations.blacksmith.casting.part;

import java.util.Optional;
import java.util.UUID;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartSnapshotCodec;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualDataCodec;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

public final class RoughMetalPartStateCodec {
    public static final String ROOT_KEY = "CraftboundRoughMetalPart";
    private static final Logger LOGGER = LogUtils.getLogger();
    private RoughMetalPartStateCodec() {}

    public static Optional<RoughMetalPartState> read(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag t = root.getCompound(ROOT_KEY);
        try {
            if (!requiredTypes(t)) return invalid("missing or mistyped field");
            Optional<MetalPartDefinitionSnapshot> snapshot = MetalPartSnapshotCodec.read(t.getCompound("DefinitionSnapshot"));
            if (snapshot.isEmpty()) return invalid("invalid definition snapshot");
            Optional<MetalVisualData> visual = MetalVisualDataCodec.read(t.getCompound("Visual"));
            if (visual.isEmpty()) return invalid("invalid visual data");
            return Optional.of(new RoughMetalPartState(t.getInt("Version"), t.getUUID("CastingResultId"),
                t.getUUID("CastingOperatorId"), id(t, "Definition"), id(t, "Metal"), id(t, "Output"),
                t.getInt("IngredientCount"), t.getLong("HeatingTicks"), t.getInt("HeatingScore"),
                t.getLong("CoolingTicksAtRemoval"), t.getInt("EffectiveBreakOnHit"),
                snapshot.get(), visual.get()));
        } catch (RuntimeException exception) { return invalid(exception.getMessage()); }
    }

    public static void write(ItemStack stack, RoughMetalPartState s) {
        CompoundTag t = new CompoundTag(); t.putInt("Version", s.version());
        t.putUUID("CastingResultId", s.castingResultId()); t.putUUID("CastingOperatorId", s.castingOperatorId());
        t.putString("Definition", s.definitionId().toString()); t.putString("Metal", s.metalId().toString());
        t.putString("Output", s.outputItemId().toString()); t.putInt("IngredientCount", s.ingredientCount());
        t.putLong("HeatingTicks", s.heatingTicks()); t.putInt("HeatingScore", s.heatingScore());
        t.putLong("CoolingTicksAtRemoval", s.coolingTicksAtRemoval()); t.putInt("EffectiveBreakOnHit", s.effectiveBreakOnHit());
        t.put("DefinitionSnapshot", MetalPartSnapshotCodec.write(s.definitionSnapshot()));
        t.put("Visual", MetalVisualDataCodec.write(s.visualData()));
        stack.getOrCreateTag().put(ROOT_KEY, t);
    }

    private static boolean requiredTypes(CompoundTag t) {
        return t.contains("Version", Tag.TAG_INT) && t.hasUUID("CastingResultId") && t.hasUUID("CastingOperatorId")
            && t.contains("Definition", Tag.TAG_STRING) && t.contains("Metal", Tag.TAG_STRING)
            && t.contains("Output", Tag.TAG_STRING) && t.contains("IngredientCount", Tag.TAG_INT)
            && t.contains("HeatingTicks", Tag.TAG_LONG) && t.contains("HeatingScore", Tag.TAG_INT)
            && t.contains("CoolingTicksAtRemoval", Tag.TAG_LONG) && t.contains("EffectiveBreakOnHit", Tag.TAG_INT)
            && t.contains("DefinitionSnapshot", Tag.TAG_COMPOUND)
            && t.contains("Visual", Tag.TAG_COMPOUND);
    }
    private static ResourceLocation id(CompoundTag t, String key) {
        ResourceLocation id = ResourceLocation.tryParse(t.getString(key));
        if (id == null || !t.getString(key).contains(":")) throw new IllegalArgumentException("invalid " + key);
        return id;
    }
    private static Optional<RoughMetalPartState> invalid(String reason) {
        LOGGER.warn("Invalid rough metal part state data: {}", reason); return Optional.empty();
    }
}
