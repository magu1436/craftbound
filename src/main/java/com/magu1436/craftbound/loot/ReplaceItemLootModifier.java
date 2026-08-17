package com.magu1436.craftbound.loot;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

/** 生成済みLootに含まれる対象Itemを、設定されたItem群へ置換する. */
public final class ReplaceItemLootModifier extends LootModifier {

    private static final Codec<List<Replacement>> REPLACEMENTS_CODEC = Replacement.CODEC.listOf()
            .flatXmap(ReplaceItemLootModifier::validateReplacements, ReplaceItemLootModifier::validateReplacements);

    public static final Codec<ReplaceItemLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).and(instance.group(
                    ResourceLocation.CODEC.fieldOf("target_item")
                            .forGetter(modifier -> modifier.targetItemId),
                    REPLACEMENTS_CODEC.fieldOf("replacements")
                            .forGetter(modifier -> modifier.replacements)
            )).apply(instance, ReplaceItemLootModifier::new));

    private final ResourceLocation targetItemId;
    private final List<Replacement> replacements;

    private ReplaceItemLootModifier(
            LootItemCondition[] conditions,
            ResourceLocation targetItemId,
            List<Replacement> replacements
    ) {
        super(conditions);
        this.targetItemId = targetItemId;
        this.replacements = List.copyOf(replacements);
    }

    private static DataResult<List<Replacement>> validateReplacements(List<Replacement> replacements) {
        return replacements.isEmpty()
                ? DataResult.error(() -> "replacements must contain at least one entry")
                : DataResult.success(replacements);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!ForgeRegistries.ITEMS.containsKey(targetItemId)) {
            return generatedLoot;
        }

        Item targetItem = ForgeRegistries.ITEMS.getValue(targetItemId);
        if (targetItem == null) {
            return generatedLoot;
        }

        long removedCount;
        try {
            removedCount = countTargetItems(generatedLoot, targetItem);
        } catch (ArithmeticException exception) {
            return generatedLoot;
        }
        if (removedCount == 0L) {
            return generatedLoot;
        }

        List<ItemStack> replacementStacks = createReplacementStacks(removedCount);
        if (replacementStacks == null) {
            return generatedLoot;
        }

        generatedLoot.removeIf(stack -> stack.is(targetItem));
        generatedLoot.addAll(replacementStacks);
        return generatedLoot;
    }

    private static long countTargetItems(ObjectArrayList<ItemStack> generatedLoot, Item targetItem) {
        long count = 0L;
        for (ItemStack stack : generatedLoot) {
            if (stack.is(targetItem)) {
                count = Math.addExact(count, stack.getCount());
            }
        }
        return count;
    }

    private List<ItemStack> createReplacementStacks(long removedCount) {
        List<ItemStack> result = new ArrayList<>();
        try {
            for (Replacement replacement : replacements) {
                if (replacement.itemId().equals(targetItemId)
                        || !ForgeRegistries.ITEMS.containsKey(replacement.itemId())) {
                    return null;
                }

                Item replacementItem = ForgeRegistries.ITEMS.getValue(replacement.itemId());
                if (replacementItem == null) {
                    return null;
                }

                long remainingCount = Math.multiplyExact(removedCount, replacement.count());
                int maximumStackSize = replacementItem.getMaxStackSize();
                while (remainingCount > 0L) {
                    int stackSize = (int) Math.min(remainingCount, maximumStackSize);
                    result.add(new ItemStack(replacementItem, stackSize));
                    remainingCount -= stackSize;
                }
            }
        } catch (ArithmeticException exception) {
            return null;
        }
        return result;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    public record Replacement(ResourceLocation itemId, int count) {

        private static final Codec<Replacement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Replacement::itemId),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Replacement::count)
        ).apply(instance, Replacement::new));
    }
}
