package com.magu1436.craftbound.occupations.foodproducer.ranch;

import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;

/** 牧畜ブロック1個が管理する動物種と、その種へ使用できる餌を定義する。 */
public enum RanchTarget {

    UNSET("unset", Set.of()),
    COW("cow", Set.of(Items.WHEAT)),
    PIG("pig", Set.of(Items.CARROT, Items.POTATO, Items.BEETROOT)),
    SHEEP("sheep", Set.of(Items.WHEAT)),
    CHICKEN("chicken", Set.of(
            Items.WHEAT_SEEDS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.TORCHFLOWER_SEEDS,
            Items.PITCHER_POD
    )),
    RABBIT("rabbit", Set.of(Items.CARROT, Items.GOLDEN_CARROT, Items.DANDELION)),
    MOOSHROOM("mooshroom", Set.of(Items.WHEAT));

    private static final RanchTarget[] VALUES = values();

    private final String serializedName;
    private final Set<Item> foods;

    RanchTarget(String serializedName, Set<Item> foods) {
        this.serializedName = serializedName;
        this.foods = foods;
    }

    public int id() {
        return ordinal();
    }

    public String serializedName() {
        return serializedName;
    }

    public String translationKey() {
        return "ranch_target.craftbound." + serializedName;
    }

    public boolean accepts(ItemStack stack) {
        return this != UNSET && foods.contains(stack.getItem());
    }

    public boolean matches(Animal animal) {
        return switch (this) {
            case COW -> animal instanceof Cow && !(animal instanceof MushroomCow);
            case PIG -> animal instanceof Pig;
            case SHEEP -> animal instanceof Sheep;
            case CHICKEN -> animal instanceof Chicken;
            case RABBIT -> animal instanceof Rabbit;
            case MOOSHROOM -> animal instanceof MushroomCow;
            case UNSET -> false;
        };
    }

    public RanchTarget next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public RanchTarget previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }

    public static RanchTarget fromId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : UNSET;
    }

    public static RanchTarget fromSerializedName(String name) {
        for (RanchTarget target : VALUES) {
            if (target.serializedName.equals(name)) {
                return target;
            }
        }
        return UNSET;
    }
}
