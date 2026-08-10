package com.magu1436.craftbound.occupations.blacksmith.casting.definition;

import java.util.Optional;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartItem;

public final class MetalPartSnapshotCodec {
    private MetalPartSnapshotCodec() {}

    public static CompoundTag write(MetalPartDefinitionSnapshot s) {
        CompoundTag t = new CompoundTag();
        putId(t, "Definition", s.definitionId()); putId(t, "Metal", s.metalId());
        t.putInt("IngredientCount", s.ingredientCount()); putId(t, "Mold", s.moldItemId());
        putId(t, "RoughOutput", s.roughOutputItemId());
        putId(t, "Output", s.outputItemId());
        CompoundTag f = new CompoundTag(); putId(f, "Item", s.failureLump().itemId());
        f.putInt("Count", s.failureLump().count()); f.putInt("UnitsPerItem", s.failureLump().unitsPerItem());
        t.put("FailureLump", f);
        CompoundTag c = new CompoundTag(); c.putLong("SurfaceSolidTicks", s.cooling().surfaceSolidTicks());
        c.putLong("SafeTicks", s.cooling().safeTicks()); c.putInt("MinimumBreakOnHit", s.cooling().minimumBreakOnHit());
        putId(c, "Evaluator", s.cooling().evaluatorId()); t.put("Cooling", c);
        CompoundTag g = new CompoundTag(); g.putDouble("StrengthMin", s.forging().strengthMin());
        g.putDouble("StrengthMax", s.forging().strengthMax()); g.putDouble("StrengthPenaltyPerPoint", s.forging().strengthPenaltyPerPoint());
        g.putInt("IdealHits", s.forging().idealHits()); g.putDouble("HitCountPenalty", s.forging().hitCountPenalty());
        g.putInt("BreakOnHit", s.forging().breakOnHit()); g.putDouble("StrengthWeight", s.forging().strengthWeight());
        g.putDouble("HitCountWeight", s.forging().hitCountWeight()); putId(g, "Evaluator", s.forging().evaluatorId()); t.put("Forging", g);
        CompoundTag q = new CompoundTag(); q.putDouble("HeatingWeight", s.partQuality().heatingWeight());
        q.putDouble("ForgingWeight", s.partQuality().forgingWeight()); putId(q, "Evaluator", s.partQuality().evaluatorId()); t.put("PartQuality", q);
        return t;
    }

    public static Optional<MetalPartDefinitionSnapshot> read(CompoundTag t) {
        try {
            ResourceLocation definition = readId(t, "Definition"), metal = readId(t, "Metal");
            ResourceLocation mold = readId(t, "Mold"), roughOutput = readId(t, "RoughOutput");
            ResourceLocation output = readId(t, "Output");
            CompoundTag f = t.getCompound("FailureLump"), c = t.getCompound("Cooling");
            CompoundTag g = t.getCompound("Forging"), q = t.getCompound("PartQuality");
            FailureLumpDefinition failure = new FailureLumpDefinition(readId(f, "Item"), f.getInt("Count"), f.getInt("UnitsPerItem"));
            CoolingDefinition cooling = new CoolingDefinition(c.getLong("SurfaceSolidTicks"), c.getLong("SafeTicks"), c.getInt("MinimumBreakOnHit"), readId(c, "Evaluator"));
            ForgingDefinition forging = new ForgingDefinition(g.getDouble("StrengthMin"), g.getDouble("StrengthMax"), g.getDouble("StrengthPenaltyPerPoint"), g.getInt("IdealHits"), g.getDouble("HitCountPenalty"), g.getInt("BreakOnHit"), g.getDouble("StrengthWeight"), g.getDouble("HitCountWeight"), readId(g, "Evaluator"));
            PartQualityDefinition quality = new PartQualityDefinition(q.getDouble("HeatingWeight"), q.getDouble("ForgingWeight"), readId(q, "Evaluator"));
            MetalPartDefinitionSnapshot snapshot = new MetalPartDefinitionSnapshot(definition, metal, t.getInt("IngredientCount"), mold, roughOutput, output, failure, cooling, forging, quality);
            if (!valid(snapshot)) return Optional.empty();
            return Optional.of(snapshot);
        } catch (RuntimeException exception) { return Optional.empty(); }
    }

    private static boolean valid(MetalPartDefinitionSnapshot s) {
        if (s.roughOutputItemId() == null) return false;
        Item roughOutput = ForgeRegistries.ITEMS.getValue(s.roughOutputItemId());
        return s.definitionId() != null && s.metalId() != null && s.moldItemId() != null
            && roughOutput instanceof RoughMetalPartItem
            && s.outputItemId() != null && s.ingredientCount() >= 1 && s.failureLump().itemId() != null
            && s.failureLump().count() >= 1 && s.failureLump().unitsPerItem() >= 1
            && s.cooling().surfaceSolidTicks() >= 0 && s.cooling().safeTicks() >= s.cooling().surfaceSolidTicks()
            && s.cooling().minimumBreakOnHit() >= 1 && s.cooling().evaluatorId() != null
            && s.forging().breakOnHit() >= s.cooling().minimumBreakOnHit() && s.forging().strengthMin() <= s.forging().strengthMax()
            && s.forging().strengthWeight() >= 0 && s.forging().hitCountWeight() >= 0
            && s.forging().strengthWeight() + s.forging().hitCountWeight() > 0 && s.forging().evaluatorId() != null
            && s.partQuality().heatingWeight() >= 0 && s.partQuality().forgingWeight() >= 0
            && s.partQuality().heatingWeight() + s.partQuality().forgingWeight() > 0 && s.partQuality().evaluatorId() != null;
    }
    private static void putId(CompoundTag tag, String key, ResourceLocation id) { tag.putString(key, id.toString()); }
    private static ResourceLocation readId(CompoundTag tag, String key) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(key));
        if (id == null || !tag.getString(key).contains(":")) throw new IllegalArgumentException(key);
        return id;
    }
}
