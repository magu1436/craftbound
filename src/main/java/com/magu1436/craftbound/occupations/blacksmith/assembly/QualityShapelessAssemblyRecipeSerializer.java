package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class QualityShapelessAssemblyRecipeSerializer
    implements RecipeSerializer<QualityShapelessAssemblyRecipe> {
    @Override
    public QualityShapelessAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
            GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
        List<AssemblyIngredient> ingredients = readIngredients(
            GsonHelper.getAsJsonArray(json, "ingredients"));
        ItemStack result = QualityAssemblyRecipeSerializer.readResult(
            GsonHelper.getAsJsonObject(json, "result"));
        return new QualityShapelessAssemblyRecipe(id, group, category, ingredients, result);
    }

    @Override
    public QualityShapelessAssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        int ingredientCount = buffer.readVarInt();
        List<AssemblyIngredient> ingredients = new ArrayList<>(ingredientCount);
        for (int index = 0; index < ingredientCount; index++) {
            ingredients.add(QualityAssemblyRecipeSerializer.readIngredient(buffer));
        }
        return new QualityShapelessAssemblyRecipe(id, group, category, ingredients, buffer.readItem());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, QualityShapelessAssemblyRecipe recipe) {
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        buffer.writeVarInt(recipe.assemblyIngredients().size());
        recipe.assemblyIngredients().forEach(
            ingredient -> QualityAssemblyRecipeSerializer.writeIngredient(buffer, ingredient));
        buffer.writeItem(recipe.result());
    }

    private static List<AssemblyIngredient> readIngredients(JsonArray json) {
        if (json.isEmpty() || json.size() > 9) {
            throw new JsonParseException("ingredients must contain 1 to 9 entries");
        }
        List<AssemblyIngredient> ingredients = new ArrayList<>(json.size());
        for (JsonElement element : json) {
            if (!element.isJsonObject()) {
                throw new JsonParseException("ingredients entries must be objects");
            }
            ingredients.add(QualityAssemblyRecipeSerializer.readIngredient(element.getAsJsonObject()));
        }
        return List.copyOf(ingredients);
    }
}
