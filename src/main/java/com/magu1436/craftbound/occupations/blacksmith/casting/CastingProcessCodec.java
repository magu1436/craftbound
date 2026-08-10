package com.magu1436.craftbound.occupations.blacksmith.casting;

import java.util.Optional;
import java.util.UUID;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartSnapshotCodec;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualData;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalVisualDataCodec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

public final class CastingProcessCodec {
    private CastingProcessCodec() {
    }

    public static CompoundTag write(CastingProcess process) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", process.version());
        tag.putUUID("ProcessId", process.processId());
        tag.putUUID("OperatorId", process.operatorId());
        tag.putString("PartDefinition", process.partDefinitionId().toString());
        tag.putString("Metal", process.metalId().toString());
        tag.putInt("MetalAmount", process.metalAmount());
        tag.putLong("HeatingTicks", process.heatingTicks());
        tag.putInt("HeatingScore", process.heatingScore());
        tag.putLong("CoolingTicks", process.coolingTicks());
        tag.putBoolean(
            "SurfaceSolidificationNotified",
            process.surfaceSolidificationNotified()
        );
        tag.put(
            "DefinitionSnapshot",
            MetalPartSnapshotCodec.write(process.definitionSnapshot())
        );
        tag.put("Visual", MetalVisualDataCodec.write(process.visualData()));
        return tag;
    }

    public static Optional<CastingProcess> read(CompoundTag tag) {
        try {
            if (!hasRequiredFields(tag)) {
                return Optional.empty();
            }

            Optional<MetalPartDefinitionSnapshot> snapshot =
                MetalPartSnapshotCodec.read(tag.getCompound("DefinitionSnapshot"));
            Optional<MetalVisualData> visual =
                MetalVisualDataCodec.read(tag.getCompound("Visual"));
            if (snapshot.isEmpty() || visual.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new CastingProcess(
                tag.getInt("Version"),
                tag.getUUID("ProcessId"),
                tag.getUUID("OperatorId"),
                readId(tag, "PartDefinition"),
                readId(tag, "Metal"),
                tag.getInt("MetalAmount"),
                tag.getLong("HeatingTicks"),
                tag.getInt("HeatingScore"),
                tag.getLong("CoolingTicks"),
                tag.getBoolean("SurfaceSolidificationNotified"),
                snapshot.get(),
                visual.get()
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private static boolean hasRequiredFields(CompoundTag tag) {
        return tag.contains("Version", Tag.TAG_INT)
            && tag.hasUUID("ProcessId")
            && tag.hasUUID("OperatorId")
            && tag.contains("PartDefinition", Tag.TAG_STRING)
            && tag.contains("Metal", Tag.TAG_STRING)
            && tag.contains("MetalAmount", Tag.TAG_INT)
            && tag.contains("HeatingTicks", Tag.TAG_LONG)
            && tag.contains("HeatingScore", Tag.TAG_INT)
            && tag.contains("CoolingTicks", Tag.TAG_LONG)
            && tag.contains("SurfaceSolidificationNotified", Tag.TAG_BYTE)
            && tag.contains("DefinitionSnapshot", Tag.TAG_COMPOUND)
            && tag.contains("Visual", Tag.TAG_COMPOUND);
    }

    private static ResourceLocation readId(CompoundTag tag, String key) {
        String value = tag.getString(key);
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) {
            throw new IllegalArgumentException("invalid " + key);
        }
        return id;
    }
}
