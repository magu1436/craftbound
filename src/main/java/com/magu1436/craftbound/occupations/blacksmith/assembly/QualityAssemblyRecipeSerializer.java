package com.magu1436.craftbound.occupations.blacksmith.assembly;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalMaterialDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.carving.definition.NonMetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.material.MetalMaterialDefinitions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.ForgeRegistries;

public final class QualityAssemblyRecipeSerializer implements RecipeSerializer<QualityAssemblyRecipe> {
    private static final int MAX_GRID_SIZE = 3;

    @Override
    public QualityAssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
            GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
        Map<Character, AssemblyIngredient> key = readKey(GsonHelper.getAsJsonObject(json, "key"));
        Pattern pattern = readPattern(GsonHelper.getAsJsonArray(json, "pattern"), key);
        ItemStack result = readResult(GsonHelper.getAsJsonObject(json, "result"));
        return new QualityAssemblyRecipe(id, group, category, pattern.width(), pattern.height(),
            pattern.ingredients(), result);
    }

    @Override
    public QualityAssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        String group = buffer.readUtf();
        CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
        List<AssemblyIngredient> ingredients = new ArrayList<>(width * height);
        for (int index = 0; index < width * height; index++) ingredients.add(readIngredient(buffer));
        return new QualityAssemblyRecipe(id, group, category, width, height, ingredients, buffer.readItem());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, QualityAssemblyRecipe recipe) {
        buffer.writeVarInt(recipe.width());
        buffer.writeVarInt(recipe.height());
        buffer.writeUtf(recipe.getGroup());
        buffer.writeEnum(recipe.category());
        recipe.assemblyIngredients().forEach(ingredient -> writeIngredient(buffer, ingredient));
        buffer.writeItem(recipe.result());
    }

    private static Map<Character, AssemblyIngredient> readKey(JsonObject json) {
        Map<Character, AssemblyIngredient> key = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String symbol = entry.getKey();
            require(symbol.length() == 1 && symbol.charAt(0) != ' ',
                "key symbols must be one non-space character");
            require(entry.getValue().isJsonObject(), "key entry `" + symbol + "` must be an object");
            key.put(symbol.charAt(0), readIngredient(entry.getValue().getAsJsonObject()));
        }
        return key;
    }

    private static Pattern readPattern(JsonArray json, Map<Character, AssemblyIngredient> key) {
        require(json.size() >= 1 && json.size() <= MAX_GRID_SIZE, "pattern must have 1 to 3 rows");
        List<String> rows = new ArrayList<>();
        int rowWidth = -1;
        for (JsonElement element : json) {
            require(element.isJsonPrimitive() && element.getAsJsonPrimitive().isString(),
                "pattern rows must be strings");
            String row = element.getAsString();
            require(rowWidth < 0 || row.length() == rowWidth, "pattern rows must have equal width");
            rowWidth = row.length();
            rows.add(row);
        }
        int firstRow = 0;
        while (firstRow < rows.size() && rows.get(firstRow).trim().isEmpty()) firstRow++;
        int lastRow = rows.size() - 1;
        while (lastRow >= firstRow && rows.get(lastRow).trim().isEmpty()) lastRow--;
        require(firstRow <= lastRow, "pattern must not be empty");
        int left = Integer.MAX_VALUE;
        int right = -1;
        for (int row = firstRow; row <= lastRow; row++) {
            String value = rows.get(row);
            require(value.length() <= MAX_GRID_SIZE, "pattern must fit within a 3x3 grid");
            for (int index = 0; index < value.length(); index++) {
                if (value.charAt(index) != ' ') {
                    left = Math.min(left, index);
                    right = Math.max(right, index);
                }
            }
        }
        int width = right - left + 1;
        int height = lastRow - firstRow + 1;
        Set<Character> used = new HashSet<>();
        List<AssemblyIngredient> ingredients = new ArrayList<>(width * height);
        for (int row = firstRow; row <= lastRow; row++) {
            String value = rows.get(row);
            for (int column = left; column <= right; column++) {
                char symbol = column < value.length() ? value.charAt(column) : ' ';
                if (symbol == ' ') {
                    ingredients.add(null);
                } else {
                    AssemblyIngredient ingredient = key.get(symbol);
                    require(ingredient != null, "pattern references undefined symbol `" + symbol + "`");
                    used.add(symbol);
                    ingredients.add(ingredient);
                }
            }
        }
        require(used.containsAll(key.keySet()), "key contains a symbol not used by pattern");
        return new Pattern(width, height, ingredients);
    }

    private static AssemblyIngredient readIngredient(JsonObject json) {
        String kind = GsonHelper.getAsString(json, "kind");
        return switch (kind) {
            case "ingredient" -> {
                require(!json.has("contributes_to_quality"),
                    "ingredient must not define contributes_to_quality");
                JsonObject vanillaJson = json.deepCopy();
                vanillaJson.remove("kind");
                yield new VanillaAssemblyIngredient(Ingredient.fromJson(vanillaJson));
            }
            case "metal_part" -> {
                ResourceLocation partType = requiredId(json, "part_type");
                ResourceLocation material = requiredId(json, "material");
                require(MetalPartDefinitions.INSTANCE.get(partType).isPresent(),
                    "unknown metal part type `" + partType + "`");
                require(MetalMaterialDefinitions.INSTANCE.get(material).isPresent(),
                    "unknown metal material `" + material + "`");
                yield new MetalPartAssemblyIngredient(partType, material,
                    GsonHelper.getAsBoolean(json, "contributes_to_quality", false));
            }
            case "nonmetal_part" -> {
                ResourceLocation partType = requiredId(json, "part_type");
                ResourceLocation material = requiredId(json, "material");
                require(NonMetalPartDefinitions.INSTANCE.get(partType).isPresent(),
                    "unknown non-metal part type `" + partType + "`");
                require(NonMetalMaterialDefinitions.INSTANCE.get(material).isPresent(),
                    "unknown non-metal material `" + material + "`");
                yield new NonMetalPartAssemblyIngredient(partType, material,
                    GsonHelper.getAsBoolean(json, "contributes_to_quality", false));
            }
            default -> throw new JsonParseException("unsupported assembly ingredient kind `" + kind + "`");
        };
    }

    private static ItemStack readResult(JsonObject json) {
        ResourceLocation itemId = requiredId(json, "item");
        require(ForgeRegistries.ITEMS.containsKey(itemId), "unknown result item `" + itemId + "`");
        int count = GsonHelper.getAsInt(json, "count", 1);
        require(count >= 1, "result count must be positive");
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        return new ItemStack(item, count);
    }

    private static ResourceLocation requiredId(JsonObject json, String field) {
        String value = GsonHelper.getAsString(json, field);
        ResourceLocation id = ResourceLocation.tryParse(value);
        require(id != null && value.contains(":"), field + " must be a namespaced id");
        return id;
    }

    private static AssemblyIngredient readIngredient(FriendlyByteBuf buffer) {
        return switch (buffer.readByte()) {
            case 0 -> new VanillaAssemblyIngredient(Ingredient.fromNetwork(buffer));
            case 1 -> new MetalPartAssemblyIngredient(buffer.readResourceLocation(),
                buffer.readResourceLocation(), buffer.readBoolean());
            case 2 -> new NonMetalPartAssemblyIngredient(buffer.readResourceLocation(),
                buffer.readResourceLocation(), buffer.readBoolean());
            case 3 -> null;
            default -> throw new IllegalArgumentException("unknown assembly ingredient network kind");
        };
    }

    private static void writeIngredient(FriendlyByteBuf buffer, AssemblyIngredient ingredient) {
        if (ingredient == null) {
            buffer.writeByte(3);
        } else if (ingredient instanceof VanillaAssemblyIngredient vanilla) {
            buffer.writeByte(0);
            vanilla.ingredient().toNetwork(buffer);
        } else if (ingredient instanceof MetalPartAssemblyIngredient metal) {
            buffer.writeByte(1);
            buffer.writeResourceLocation(metal.partTypeId());
            buffer.writeResourceLocation(metal.materialId());
            buffer.writeBoolean(metal.contributesToQuality());
        } else if (ingredient instanceof NonMetalPartAssemblyIngredient nonMetal) {
            buffer.writeByte(2);
            buffer.writeResourceLocation(nonMetal.partTypeId());
            buffer.writeResourceLocation(nonMetal.materialId());
            buffer.writeBoolean(nonMetal.contributesToQuality());
        } else {
            throw new IllegalArgumentException("unsupported assembly ingredient type");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new JsonParseException(message);
    }

    private record Pattern(int width, int height, List<AssemblyIngredient> ingredients) {}
}
