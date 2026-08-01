package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.magu1436.craftbound.Craftbound;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 初期中間素材のコード定義レシピ。データ駆動化は正式レシピ調整段階で行う。 */
public final class FoodProcessingRecipes {

    private static final Set<Item> RAW_MEATS = Set.of(
            Items.BEEF, Items.PORKCHOP, Items.MUTTON, Items.CHICKEN, Items.RABBIT
    );
    private static final Set<Item> VEGETABLES = Set.of(
            Items.CARROT, Items.POTATO, Items.BEETROOT, Items.PUMPKIN
    );
    private static final Set<Item> BERRIES = Set.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES);

    private FoodProcessingRecipes() {
    }

    public static Optional<Match> find(FoodProcessingOperation operation, List<ItemStack> inputs) {
        return switch (operation) {
            case CUT -> cut(inputs);
            case GRIND -> grind(inputs);
            case PRESERVE -> preserve(inputs);
            case MIX -> mix(inputs);
            case HEAT -> heat(inputs);
        };
    }

    private static Optional<Match> cut(List<ItemStack> inputs) {
        int slot = onlyOccupiedSlot(inputs);
        if (slot < 0) return Optional.empty();
        ItemStack input = inputs.get(slot);
        ItemStack output;
        int required;
        if (RAW_MEATS.contains(input.getItem())) {
            output = new ItemStack(Craftbound.SLICED_MEAT.get(), 2);
            FoodIntermediateData.setSource(output, input.getItem());
            required = 1;
        } else if (VEGETABLES.contains(input.getItem())) {
            output = new ItemStack(Craftbound.CHOPPED_VEGETABLE.get(), 2);
            FoodIntermediateData.setSource(output, input.getItem());
            required = 1;
        } else if (input.is(Items.APPLE) || BERRIES.contains(input.getItem())) {
            output = new ItemStack(Craftbound.FRUIT_PIECES.get(), 2);
            FoodIntermediateData.setSource(output, input.getItem());
            required = input.is(Items.APPLE) ? 1 : 2;
        } else {
            return Optional.empty();
        }
        if (input.getCount() < required) return Optional.empty();
        return Optional.of(match(output, slot, required, true, inputs));
    }

    private static Optional<Match> grind(List<ItemStack> inputs) {
        int slot = onlyOccupiedSlot(inputs);
        if (slot < 0) return Optional.empty();
        ItemStack input = inputs.get(slot);
        if (input.is(Items.WHEAT) && input.getCount() >= 2) {
            return Optional.of(match(
                    new ItemStack(Craftbound.WHEAT_FLOUR.get()), slot, 2, false, inputs
            ));
        }
        if (input.is(Craftbound.SLICED_MEAT.get()) && input.getCount() >= 2) {
            ItemStack output = new ItemStack(Craftbound.GROUND_MEAT.get());
            FoodIntermediateData.copySource(input, output);
            return Optional.of(match(output, slot, 2, false, inputs));
        }
        return Optional.empty();
    }

    private static Optional<Match> preserve(List<ItemStack> inputs) {
        int slot = onlyOccupiedSlot(inputs);
        if (slot < 0) return Optional.empty();
        ItemStack input = inputs.get(slot);
        ItemStack output;
        int required;
        if (input.is(Craftbound.SLICED_MEAT.get())) {
            output = new ItemStack(Craftbound.DRIED_MEAT.get());
            required = 1;
        } else if (input.is(Craftbound.CHOPPED_VEGETABLE.get())) {
            output = new ItemStack(Craftbound.DRIED_VEGETABLE.get());
            required = 2;
        } else if (input.is(Craftbound.FRUIT_PIECES.get())) {
            output = new ItemStack(Craftbound.DRIED_FRUIT.get());
            required = 2;
        } else {
            return Optional.empty();
        }
        if (input.getCount() < required) return Optional.empty();
        FoodIntermediateData.copySource(input, output);
        return Optional.of(match(output, slot, required, false, inputs));
    }

    private static Optional<Match> mix(List<ItemStack> inputs) {
        int flourSlot = findSlot(inputs, Craftbound.WHEAT_FLOUR.get());
        int waterSlot = findSlot(inputs, Items.POTION);
        if (flourSlot < 0 || waterSlot < 0 || flourSlot == waterSlot || occupiedCount(inputs) != 2) {
            return Optional.empty();
        }
        ItemStack water = inputs.get(waterSlot);
        if (!net.minecraft.world.item.alchemy.PotionUtils.getPotion(water)
                .equals(net.minecraft.world.item.alchemy.Potions.WATER)) {
            return Optional.empty();
        }
        int[] consumed = new int[3];
        consumed[flourSlot] = 1;
        consumed[waterSlot] = 1;
        return Optional.of(new Match(
                new ItemStack(Craftbound.DOUGH.get()),
                consumed,
                new ItemStack(Items.GLASS_BOTTLE),
                false,
                qualityInputs(inputs, consumed)
        ));
    }

    private static Optional<Match> heat(List<ItemStack> inputs) {
        int slot = onlyOccupiedSlot(inputs);
        if (slot < 0) return Optional.empty();
        ItemStack input = inputs.get(slot);
        if (!FoodCookingData.isPreparedSet(input)) return Optional.empty();
        ItemStack output = FoodCookingData.createDish(input);
        int[] consumed = new int[3];
        consumed[slot] = 1;
        return Optional.of(new Match(
                output,
                consumed,
                FoodCookingData.returnedContainer(input),
                false,
                qualityInputs(inputs, consumed)
        ));
    }

    private static Match match(
            ItemStack output,
            int slot,
            int count,
            boolean toolRequired,
            List<ItemStack> inputs
    ) {
        int[] consumed = new int[3];
        consumed[slot] = count;
        return new Match(
                output,
                consumed,
                ItemStack.EMPTY,
                toolRequired,
                qualityInputs(inputs, consumed)
        );
    }

    private static List<ItemStack> qualityInputs(List<ItemStack> inputs, int[] consumed) {
        List<ItemStack> qualityInputs = new ArrayList<>();
        for (int slot = 0; slot < consumed.length; slot++) {
            if (consumed[slot] > 0) {
                ItemStack copy = inputs.get(slot).copy();
                copy.setCount(consumed[slot]);
                qualityInputs.add(copy);
            }
        }
        return qualityInputs;
    }

    private static int onlyOccupiedSlot(List<ItemStack> inputs) {
        int found = -1;
        for (int slot = 0; slot < inputs.size(); slot++) {
            if (inputs.get(slot).isEmpty()) continue;
            if (found >= 0) return -1;
            found = slot;
        }
        return found;
    }

    private static int occupiedCount(List<ItemStack> inputs) {
        return (int) inputs.stream().filter(stack -> !stack.isEmpty()).count();
    }

    private static int findSlot(List<ItemStack> inputs, Item item) {
        for (int slot = 0; slot < inputs.size(); slot++) {
            if (inputs.get(slot).is(item)) return slot;
        }
        return -1;
    }

    public record Match(
            ItemStack output,
            int[] consumed,
            ItemStack returnedContainer,
            boolean toolRequired,
            List<ItemStack> qualityInputs
    ) {
    }
}
