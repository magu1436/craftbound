package com.magu1436.craftbound.occupations.foodproducer.processing;

import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.magu1436.craftbound.Craftbound;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;

/** データパックのcraftbound_food_recipes料理定義をサーバーリロード時に読み込む。 */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FoodCookingRecipeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FoodCookingRecipeManager INSTANCE = new FoodCookingRecipeManager();
    private static volatile Map<ResourceLocation, FoodCookingRecipeDefinition> recipes = Map.of();

    private FoodCookingRecipeManager() {
        super(GSON, "craftbound_food_recipes");
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    public static Optional<FoodProcessingRecipes.Match> findMix(List<ItemStack> inputs) {
        return recipes.values().stream()
                .map(recipe -> recipe.match(inputs))
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    public static Optional<FoodCookingRecipeDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(recipes.get(id));
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        Map<ResourceLocation, FoodCookingRecipeDefinition> loaded = new LinkedHashMap<>();
        objects.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        loaded.put(entry.getKey(), FoodCookingRecipeDefinition.fromJson(
                                entry.getKey(), entry.getValue().getAsJsonObject()
                        ));
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to load Craftbound food recipe {}", entry.getKey(), exception);
                    }
                });
        recipes = Collections.unmodifiableMap(new LinkedHashMap<>(loaded));
        LOGGER.info("Loaded {} Craftbound food recipes", recipes.size());
    }
}
