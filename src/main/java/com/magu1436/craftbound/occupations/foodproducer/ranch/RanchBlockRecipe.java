package com.magu1436.craftbound.occupations.foodproducer.ranch;

import com.google.gson.JsonObject;
import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.registry.CraftboundRecipeSerializers;
import com.magu1436.craftbound.registry.CraftboundItems;
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

/** 牧畜管理取得者の手動クラフトだけに一致する、9素材の不定形レシピ。 */
public final class RanchBlockRecipe extends ShapelessRecipe {

    private static final NonNullList<Ingredient> INGREDIENTS = createIngredients();

    public RanchBlockRecipe(ResourceLocation id) {
        super(
                id,
                "",
                CraftingBookCategory.MISC,
                new ItemStack(CraftboundItems.RANCH_BLOCK.get()),
                INGREDIENTS
        );
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!hasRanchManagement(container)) {
            return false;
        }
        return super.matches(container, level);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CraftboundRecipeSerializers.RANCH_BLOCK.get();
    }

    private static boolean hasRanchManagement(CraftingContainer container) {
        if (!(container instanceof TransientCraftingContainer transientContainer)) {
            return false;
        }
        AbstractContainerMenu menu = ((TransientCraftingContainerAccessor) transientContainer)
                .craftbound$getMenu();
        if (!(menu instanceof CraftingMenu craftingMenu)) {
            return false;
        }
        if (!(((CraftingMenuAccessor) craftingMenu).craftbound$getPlayer()
                instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return FoodProducerSkills.has(serverPlayer, FoodProducerSkills.RANCH_MANAGEMENT);
    }

    private static NonNullList<Ingredient> createIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (int index = 0; index < 4; index++) {
            ingredients.add(Ingredient.of(ItemTags.PLANKS));
        }
        ingredients.add(Ingredient.of(Items.IRON_INGOT));
        ingredients.add(Ingredient.of(Items.IRON_INGOT));
        ingredients.add(Ingredient.of(Items.CHEST));
        ingredients.add(Ingredient.of(Items.HAY_BLOCK));
        ingredients.add(Ingredient.of(Items.REDSTONE));
        return ingredients;
    }

    public static final class Serializer implements RecipeSerializer<RanchBlockRecipe> {

        @Override
        public RanchBlockRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new RanchBlockRecipe(id);
        }

        @Override
        public RanchBlockRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new RanchBlockRecipe(id);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, RanchBlockRecipe recipe) {
        }
    }
}
