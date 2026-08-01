package com.magu1436.craftbound.occupations.foodproducer.processing;

import com.google.gson.JsonObject;
import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.mixin.accessor.CraftingMenuAccessor;
import com.magu1436.craftbound.mixin.accessor.TransientCraftingContainerAccessor;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** 基礎加工取得者だけが使える、開発中の加工設備レシピ。 */
public final class BasicProcessingEquipmentRecipe extends ShapelessRecipe {

    private final ResourceLocation id;

    public BasicProcessingEquipmentRecipe(ResourceLocation id) {
        super(id, "", CraftingBookCategory.MISC, result(id), ingredients(id));
        this.id = id;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return hasBasicProcessing(container) && super.matches(container, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Craftbound.BASIC_PROCESSING_EQUIPMENT_RECIPE_SERIALIZER.get();
    }

    private static boolean hasBasicProcessing(CraftingContainer container) {
        if (!(container instanceof TransientCraftingContainer transientContainer)) return false;
        AbstractContainerMenu menu = ((TransientCraftingContainerAccessor) transientContainer).craftbound$getMenu();
        if (!(menu instanceof CraftingMenu craftingMenu)) return false;
        if (!(((CraftingMenuAccessor) craftingMenu).craftbound$getPlayer() instanceof ServerPlayer player)) return false;
        return FoodProducerSkills.has(player, FoodProducerSkills.BASIC_PROCESSING);
    }

    private static ItemStack result(ResourceLocation id) {
        return switch (id.getPath()) {
            case "cooking_knife" -> new ItemStack(Craftbound.COOKING_KNIFE.get());
            case "hand_mill" -> new ItemStack(Craftbound.HAND_MILL_ITEM.get());
            case "drying_rack" -> new ItemStack(Craftbound.DRYING_RACK_ITEM.get());
            case "cooking_pot" -> new ItemStack(Craftbound.COOKING_POT_ITEM.get());
            default -> new ItemStack(Craftbound.COOKING_TABLE_ITEM.get());
        };
    }

    private static NonNullList<Ingredient> ingredients(ResourceLocation id) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        switch (id.getPath()) {
            case "cooking_knife" -> {
                ingredients.add(Ingredient.of(Items.IRON_INGOT));
                ingredients.add(Ingredient.of(Items.STICK));
            }
            case "hand_mill" -> {
                add(ingredients, Ingredient.of(Items.COBBLESTONE), 4);
                ingredients.add(Ingredient.of(Items.IRON_INGOT));
            }
            case "drying_rack" -> {
                add(ingredients, Ingredient.of(ItemTags.PLANKS), 4);
                add(ingredients, Ingredient.of(Items.STRING), 2);
            }
            case "cooking_pot" -> {
                add(ingredients, Ingredient.of(Items.IRON_INGOT), 5);
                ingredients.add(Ingredient.of(Items.BOWL));
            }
            default -> {
                add(ingredients, Ingredient.of(ItemTags.PLANKS), 4);
                ingredients.add(Ingredient.of(Items.CRAFTING_TABLE));
                ingredients.add(Ingredient.of(Items.BOWL));
            }
        }
        return ingredients;
    }

    private static void add(NonNullList<Ingredient> list, Ingredient ingredient, int count) {
        for (int index = 0; index < count; index++) list.add(ingredient);
    }

    public static final class Serializer implements RecipeSerializer<BasicProcessingEquipmentRecipe> {
        @Override
        public BasicProcessingEquipmentRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new BasicProcessingEquipmentRecipe(id);
        }

        @Override
        public BasicProcessingEquipmentRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new BasicProcessingEquipmentRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BasicProcessingEquipmentRecipe recipe) {
        }
    }
}
