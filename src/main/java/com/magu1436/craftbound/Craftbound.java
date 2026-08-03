package com.magu1436.craftbound;

import com.magu1436.craftbound.client.event.CraftboundMovementPenaltyEventHandler;
import com.magu1436.craftbound.occupations.adventurer.AdventurerConfig;
import com.magu1436.craftbound.occupations.adventurer.client.AdventurerMovementPenaltyRules;
import com.magu1436.craftbound.occupations.adventurer.client.AdventurerMovementPenaltyService;
import com.magu1436.craftbound.occupations.adventurer.experience.AdventurerMobKillExperienceSource;
import com.magu1436.craftbound.occupations.adventurer.rewards.AdventurerRewardsFactory;
import com.magu1436.craftbound.occupations.architect.rewards.ArchitectRewardFactory;
import com.magu1436.craftbound.occupations.architect.experience.ArchitectConstructionExperienceSource;
import com.magu1436.craftbound.occupations.architect.ArchitectConfig;
import com.magu1436.craftbound.occupations.explorer.rewards.ExplorerRewardsFactory;
import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.registry.CraftboundAttributes;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.registry.CraftboundBlocks;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.registry.CraftboundMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Craftbound.MODID)
public class Craftbound
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "craftbound";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public Craftbound(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        context.registerConfig(
            ModConfig.Type.SERVER,
            AdventurerConfig.SPEC,
            "craftbound-adventurer.toml"
        );
        context.registerConfig(
            ModConfig.Type.SERVER,
            ArchitectConfig.SPEC,
            "craftbound-architect.toml"
        );

        // ブロックやアイテム関連のレジストリの登録
        CraftboundItems.register(modEventBus);
        CraftboundBlocks.register(modEventBus);
        CraftboundBlockEntities.register(modEventBus);
        CraftboundMenus.register(modEventBus);

        // Attributes の登録
        CraftboundAttributes.register(modEventBus);

        // 経験値源の登録
        AdventurerMobKillExperienceSource.register();
        ArchitectConstructionExperienceSource.register();

        // 報酬の登録
        AdventurerRewardsFactory.registerRewards();
        ArchitectRewardFactory.registerRewards();
        ExplorerRewardsFactory.registerRewards();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(CraftboundNetwork::register);

        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
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
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            event.enqueueWork(
                ClientModEvents::registerMovementPenaltyEventHandler
            );
        }

        private static void registerMovementPenaltyEventHandler()
        {
            AdventurerMovementPenaltyService adventurerService =
                new AdventurerMovementPenaltyService(
                    AdventurerMovementPenaltyRules.create()
                );

            MinecraftForge.EVENT_BUS.register(
                new CraftboundMovementPenaltyEventHandler(
                    adventurerService
                )
            );
        }
    }
}
