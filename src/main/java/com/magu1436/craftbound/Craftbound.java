package com.magu1436.craftbound;

import com.mojang.logging.LogUtils;
import com.magu1436.craftbound.occupations.foodproducer.farming.AgriculturalFertilizerItem;
import com.magu1436.craftbound.occupations.foodproducer.loot.FoodProducerLootModifiers;
import com.magu1436.craftbound.occupations.foodproducer.processing.BasicProcessingEquipmentRecipe;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodIntermediateItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlock;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingMenu;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingScreen;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodDishItem;
import com.magu1436.craftbound.occupations.foodproducer.processing.PreparedIngredientSetItem;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlock;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockRecipe;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchMenu;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Craftbound.MODID)
public class Craftbound
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "craftbound";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "craftbound" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "craftbound" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "craftbound" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creates a new Block with the id "craftbound:example_block", combining the namespace and path
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    // Creates a new BlockItem with the id "craftbound:example_block", combining the namespace and path
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));

    // Creates a new food item with the id "craftbound:example_id", nutrition 1 and saturation 2
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));

    public static final RegistryObject<Item> AGRICULTURAL_FERTILIZER = ITEMS.register(
            "agricultural_fertilizer",
            () -> new AgriculturalFertilizerItem(new Item.Properties())
    );
    public static final RegistryObject<Item> COMPOST = ITEMS.register(
            "compost",
            () -> new Item(new Item.Properties())
    );
    public static final RegistryObject<Item> COOKING_KNIFE = ITEMS.register(
            "cooking_knife", () -> new Item(new Item.Properties().durability(256))
    );
    public static final RegistryObject<Item> WHEAT_FLOUR = intermediate("wheat_flour");
    public static final RegistryObject<Item> DOUGH = intermediate("dough");
    public static final RegistryObject<Item> SLICED_MEAT = intermediate("sliced_meat");
    public static final RegistryObject<Item> GROUND_MEAT = intermediate("ground_meat");
    public static final RegistryObject<Item> CHOPPED_VEGETABLE = intermediate("chopped_vegetable");
    public static final RegistryObject<Item> FRUIT_PIECES = intermediate("fruit_pieces");
    public static final RegistryObject<Item> DRIED_MEAT = intermediate("dried_meat");
    public static final RegistryObject<Item> DRIED_VEGETABLE = intermediate("dried_vegetable");
    public static final RegistryObject<Item> DRIED_FRUIT = intermediate("dried_fruit");
    public static final RegistryObject<Item> PREPARED_INGREDIENT_SET = ITEMS.register(
            "prepared_ingredient_set", () -> new PreparedIngredientSetItem(new Item.Properties())
    );
    public static final RegistryObject<Item> FOOD_DISH = ITEMS.register(
            "food_dish", () -> new FoodDishItem(new Item.Properties().stacksTo(16).food(
                    new FoodProperties.Builder().nutrition(1).saturationMod(0.0F).build()
            ))
    );

    public static final RegistryObject<Block> COOKING_TABLE = processingBlock("cooking_table", MapColor.WOOD, SoundType.WOOD);
    public static final RegistryObject<Block> HAND_MILL = processingBlock("hand_mill", MapColor.STONE, SoundType.STONE);
    public static final RegistryObject<Block> DRYING_RACK = processingBlock("drying_rack", MapColor.WOOD, SoundType.WOOD);
    public static final RegistryObject<Block> COOKING_POT = processingBlock("cooking_pot", MapColor.METAL, SoundType.METAL);
    public static final RegistryObject<Item> COOKING_TABLE_ITEM = blockItem("cooking_table", COOKING_TABLE);
    public static final RegistryObject<Item> HAND_MILL_ITEM = blockItem("hand_mill", HAND_MILL);
    public static final RegistryObject<Item> DRYING_RACK_ITEM = blockItem("drying_rack", DRYING_RACK);
    public static final RegistryObject<Item> COOKING_POT_ITEM = blockItem("cooking_pot", COOKING_POT);
    public static final RegistryObject<Block> RANCH_BLOCK = BLOCKS.register(
            "ranch_block",
            () -> new RanchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD))
    );
    public static final RegistryObject<Item> RANCH_BLOCK_ITEM = ITEMS.register(
            "ranch_block",
            () -> new BlockItem(RANCH_BLOCK.get(), new Item.Properties())
    );
    public static final RegistryObject<BlockEntityType<RanchBlockEntity>> RANCH_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "ranch_block",
                    () -> BlockEntityType.Builder.of(RanchBlockEntity::new, RANCH_BLOCK.get()).build(null)
            );
    public static final RegistryObject<MenuType<RanchMenu>> RANCH_MENU = MENU_TYPES.register(
            "ranch_block",
            () -> IForgeMenuType.create(RanchMenu::new)
    );
    public static final RegistryObject<RecipeSerializer<RanchBlockRecipe>> RANCH_BLOCK_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("ranch_block", RanchBlockRecipe.Serializer::new);
    public static final RegistryObject<BlockEntityType<FoodProcessingBlockEntity>> FOOD_PROCESSING_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("food_processing", () -> BlockEntityType.Builder.of(
                    FoodProcessingBlockEntity::new,
                    COOKING_TABLE.get(), HAND_MILL.get(), DRYING_RACK.get(), COOKING_POT.get()
            ).build(null));
    public static final RegistryObject<MenuType<FoodProcessingMenu>> FOOD_PROCESSING_MENU = MENU_TYPES.register(
            "food_processing", () -> IForgeMenuType.create(FoodProcessingMenu::new)
    );
    public static final RegistryObject<RecipeSerializer<BasicProcessingEquipmentRecipe>>
            BASIC_PROCESSING_EQUIPMENT_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register(
                    "basic_processing_equipment", BasicProcessingEquipmentRecipe.Serializer::new
            );

    // Creates a creative tab with the id "craftbound:example_tab" for the example item, that is placed after the combat tab
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    public Craftbound(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        FoodProducerLootModifiers.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
        {
            event.accept(EXAMPLE_BLOCK_ITEM);
            event.accept(RANCH_BLOCK_ITEM);
            event.accept(COOKING_TABLE_ITEM);
            event.accept(HAND_MILL_ITEM);
            event.accept(DRYING_RACK_ITEM);
            event.accept(COOKING_POT_ITEM);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
        {
            event.accept(COMPOST);
            event.accept(AGRICULTURAL_FERTILIZER);
            event.accept(COOKING_KNIFE);
            event.accept(WHEAT_FLOUR);
            event.accept(DOUGH);
            event.accept(SLICED_MEAT);
            event.accept(GROUND_MEAT);
            event.accept(CHOPPED_VEGETABLE);
            event.accept(FRUIT_PIECES);
            event.accept(DRIED_MEAT);
            event.accept(DRIED_VEGETABLE);
            event.accept(DRIED_FRUIT);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            event.enqueueWork(() -> MenuScreens.register(RANCH_MENU.get(), RanchScreen::new));
            event.enqueueWork(() -> MenuScreens.register(FOOD_PROCESSING_MENU.get(), FoodProcessingScreen::new));
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    private static RegistryObject<Item> intermediate(String name) {
        return ITEMS.register(name, () -> new FoodIntermediateItem(new Item.Properties()));
    }

    private static RegistryObject<Block> processingBlock(String name, MapColor color, SoundType sound) {
        return BLOCKS.register(name, () -> new FoodProcessingBlock(BlockBehaviour.Properties.of()
                .mapColor(color).strength(2.5F).sound(sound)));
    }

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
