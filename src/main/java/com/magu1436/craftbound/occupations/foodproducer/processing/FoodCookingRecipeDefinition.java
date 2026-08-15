package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

/** データパックから読み込む、混合工程用の料理定義。 */
public record FoodCookingRecipeDefinition(
        ResourceLocation id,
        ResultType resultType,
        List<IngredientRule> inputs,
        int requiredRecipeRank,
        String nameKey,
        int nutrition,
        float saturationGain,
        List<FoodCookingEffect> effects,
        boolean preserved,
        FoodAutomationPolicy automationPolicy,
        int experience,
        ItemStack returnedContainer
) {

    public FoodCookingRecipeDefinition {
        inputs = List.copyOf(inputs);
        effects = List.copyOf(effects);
        returnedContainer = returnedContainer.copy();
    }

    public static FoodCookingRecipeDefinition fromJson(ResourceLocation id, JsonObject json) {
        ResultType resultType = ResultType.fromName(GsonHelper.getAsString(json, "result"));
        JsonArray inputJson = GsonHelper.getAsJsonArray(json, "inputs");
        if (inputJson.isEmpty() || inputJson.size() > 3) {
            throw new IllegalArgumentException("inputs must contain 1 to 3 entries");
        }
        List<IngredientRule> inputs = new ArrayList<>(inputJson.size());
        inputJson.forEach(element -> inputs.add(IngredientRule.fromJson(element.getAsJsonObject())));

        JsonObject food = GsonHelper.getAsJsonObject(json, "food");
        List<FoodCookingEffect> effects = parseEffects(json);

        ItemStack returnedContainer = ItemStack.EMPTY;
        if (json.has("returned_container")) {
            JsonObject returned = GsonHelper.getAsJsonObject(json, "returned_container");
            Item item = requiredItem(requiredId(returned, "item"));
            returnedContainer = new ItemStack(item, Math.max(1, GsonHelper.getAsInt(returned, "count", 1)));
        }

        return new FoodCookingRecipeDefinition(
                id,
                resultType,
                inputs,
                Math.max(0, Math.min(5, GsonHelper.getAsInt(json, "required_recipe_rank", 0))),
                GsonHelper.getAsString(json, "name_key"),
                Math.max(1, GsonHelper.getAsInt(food, "nutrition")),
                Math.max(0.0F, GsonHelper.getAsFloat(food, "saturation_gain")),
                effects,
                GsonHelper.getAsBoolean(json, "preserved", false),
                FoodAutomationPolicy.fromName(GsonHelper.getAsString(
                        json, "automation_policy", "manual_only"
                )),
                Math.max(0, GsonHelper.getAsInt(json, "experience", 0)),
                returnedContainer
        );
    }

    public FoodProcessingRecipes.Match match(List<ItemStack> stacks) {
        if ((int) stacks.stream().filter(stack -> !stack.isEmpty()).count() != inputs.size()) {
            return null;
        }
        int[] consumed = new int[3];
        if (!assignInput(0, stacks, new boolean[3], consumed)) {
            return null;
        }

        List<ItemStack> qualityInputs = new ArrayList<>();
        for (int slot = 0; slot < consumed.length; slot++) {
            if (consumed[slot] <= 0) continue;
            ItemStack copy = stacks.get(slot).copy();
            copy.setCount(consumed[slot]);
            qualityInputs.add(copy);
        }

        FoodQuality qualityCap = qualityInputs.stream()
                .filter(FoodQualityItems::isQualityTarget)
                .map(FoodQualityData::getOrStandard)
                .max(java.util.Comparator.comparingInt(FoodQuality::value))
                .orElse(FoodQuality.STANDARD);
        ItemStack output = new ItemStack(resultType == ResultType.PREPARED
                ? CraftboundItems.PREPARED_INGREDIENT_SET.get()
                : CraftboundItems.FOOD_DISH.get());
        FoodCookingData.write(
                output,
                id,
                nameKey,
                nutrition,
                saturationGain,
                effects,
                qualityCap,
                FoodIntermediateData.SUCCESS_RATING,
                preserved,
                experience,
                requiredRecipeRank
        );
        return new FoodProcessingRecipes.Match(
                output,
                consumed,
                returnedContainer.copy(),
                false,
                qualityInputs,
                requiredRecipeRank,
                automationPolicy
        );
    }

    public List<ItemStack> createTestInputs(long gameTime) {
        List<ItemStack> result = new ArrayList<>(inputs.size());
        for (IngredientRule input : inputs) {
            ItemStack stack = input.example();
            if (FoodQualityItems.isQualityTarget(stack)) {
                FoodQualityData.initialize(stack, FoodQuality.STANDARD, gameTime);
            }
            result.add(stack);
        }
        return result;
    }

    public ItemStack createTestDish(FoodQuality quality, long gameTime) {
        FoodProcessingRecipes.Match match = match(createTestInputs(gameTime));
        if (match == null) {
            return ItemStack.EMPTY;
        }
        ItemStack output = match.output().copy();
        if (resultType == ResultType.PREPARED) {
            output = FoodCookingData.createDish(output);
        }
        FoodQualityData.initialize(output, quality, gameTime);
        return output;
    }

    private boolean assignInput(int inputIndex, List<ItemStack> stacks, boolean[] used, int[] consumed) {
        if (inputIndex >= inputs.size()) return true;
        IngredientRule input = inputs.get(inputIndex);
        for (int slot = 0; slot < stacks.size(); slot++) {
            if (used[slot] || !input.matches(stacks.get(slot))) continue;
            used[slot] = true;
            consumed[slot] = input.count();
            if (assignInput(inputIndex + 1, stacks, used, consumed)) return true;
            consumed[slot] = 0;
            used[slot] = false;
        }
        return false;
    }

    private static ResourceLocation requiredId(JsonObject json, String key) {
        ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(json, key));
        if (id == null) throw new IllegalArgumentException("invalid resource location in " + key);
        return id;
    }

    private static List<FoodCookingEffect> parseEffects(JsonObject json) {
        if (json.has("effect") && json.has("effects")) {
            throw new IllegalArgumentException("use either effect or effects, not both");
        }
        List<FoodCookingEffect> effects = new ArrayList<>();
        if (json.has("effect")) {
            effects.add(parseEffect(GsonHelper.getAsJsonObject(json, "effect"), "mob_effect"));
        }
        if (json.has("effects")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "effects");
            for (int index = 0; index < array.size(); index++) {
                JsonObject effect = array.get(index).getAsJsonObject();
                effects.add(parseEffect(
                        effect,
                        GsonHelper.getAsString(effect, "type", "mob_effect")
                ));
            }
        }
        return List.copyOf(effects);
    }

    private static FoodCookingEffect parseEffect(JsonObject json, String type) {
        ResourceLocation effectId = requiredId(json, "id");
        var effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            throw new IllegalArgumentException("unknown effect: " + effectId);
        }
        int durationTicks = secondsToTicks(GsonHelper.getAsInt(json, "duration_seconds", 0));
        return switch (type) {
            case "mob_effect" -> new FoodCookingEffect(
                    effectId,
                    Math.max(0, GsonHelper.getAsInt(json, "amplifier", 0)),
                    durationTicks,
                    GsonHelper.getAsBoolean(json, "show_particles", true),
                    GsonHelper.getAsBoolean(json, "show_icon", true)
            );
            case "attribute_modifier" -> {
                if (!(effect instanceof FoodRoleMobEffect roleEffect)) {
                    throw new IllegalArgumentException(
                            "attribute_modifier requires a FoodRoleMobEffect: " + effectId
                    );
                }
                double amount = GsonHelper.getAsDouble(json, "amount");
                if (!Double.isFinite(amount)
                        || amount < 0.0D
                        || amount > roleEffect.maximumEffectiveAmount()) {
                    throw new IllegalArgumentException(
                            "attribute effect amount must be between 0 and "
                                    + roleEffect.maximumEffectiveAmount()
                    );
                }
                yield new FoodCookingEffect(
                        effectId,
                        roleEffect.encodeEffectiveAmount(amount),
                        durationTicks,
                        GsonHelper.getAsBoolean(json, "show_particles", false),
                        GsonHelper.getAsBoolean(json, "show_icon", false)
                );
            }
            default -> throw new IllegalArgumentException("unknown effect type: " + type);
        };
    }

    private static int secondsToTicks(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return seconds > Integer.MAX_VALUE / 20 ? Integer.MAX_VALUE : seconds * 20;
    }

    private static Item requiredItem(ResourceLocation id) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("unknown item: " + id);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) throw new IllegalArgumentException("air cannot be an ingredient");
        return item;
    }

    public enum ResultType {
        PREPARED,
        DISH;

        static ResultType fromName(String name) {
            return switch (name) {
                case "prepared" -> PREPARED;
                case "dish" -> DISH;
                default -> throw new IllegalArgumentException("unknown result type: " + name);
            };
        }
    }

    public record IngredientRule(
            List<Item> items,
            @Nullable TagKey<Item> tag,
            int count,
            @Nullable ResourceLocation sourceItem,
            @Nullable ResourceLocation potionId
    ) {
        public IngredientRule {
            items = List.copyOf(items);
        }

        static IngredientRule fromJson(JsonObject json) {
            List<Item> items = new ArrayList<>();
            if (json.has("item")) {
                items.add(requiredItem(requiredId(json, "item")));
            }
            if (json.has("items")) {
                GsonHelper.getAsJsonArray(json, "items").forEach(element -> {
                    ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                    if (id == null) throw new IllegalArgumentException("invalid item id");
                    items.add(requiredItem(id));
                });
            }
            @Nullable TagKey<Item> tag = null;
            if (json.has("tag")) {
                tag = TagKey.create(Registries.ITEM, requiredId(json, "tag"));
            }
            if (items.isEmpty() && tag == null) {
                throw new IllegalArgumentException("ingredient requires item, items, or tag");
            }
            @Nullable ResourceLocation sourceItem = json.has("source_item")
                    ? requiredId(json, "source_item") : null;
            if (sourceItem != null) requiredItem(sourceItem);
            @Nullable ResourceLocation potionId = json.has("potion")
                    ? requiredId(json, "potion") : null;
            if (potionId != null && !BuiltInRegistries.POTION.containsKey(potionId)) {
                throw new IllegalArgumentException("unknown potion: " + potionId);
            }
            return new IngredientRule(
                    items,
                    tag,
                    Math.max(1, GsonHelper.getAsInt(json, "count", 1)),
                    sourceItem,
                    potionId
            );
        }

        boolean matches(ItemStack stack) {
            if (stack.isEmpty() || stack.getCount() < count) return false;
            if (!items.contains(stack.getItem()) && (tag == null || !stack.is(tag))) return false;
            if (sourceItem != null && !FoodIntermediateData.getSource(stack).filter(sourceItem::equals).isPresent()) {
                return false;
            }
            return potionId == null
                    || BuiltInRegistries.POTION.getKey(PotionUtils.getPotion(stack)).equals(potionId);
        }

        ItemStack example() {
            if (items.isEmpty()) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(items.get(0), count);
            if (sourceItem != null) {
                FoodIntermediateData.setSource(stack, requiredItem(sourceItem));
            }
            if (potionId != null) {
                PotionUtils.setPotion(stack, BuiltInRegistries.POTION.get(potionId));
            }
            return stack;
        }
    }
}
