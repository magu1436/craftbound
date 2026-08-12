package com.magu1436.craftbound;

import com.magu1436.craftbound.client.event.CraftboundMovementPenaltyEventHandler;
import com.magu1436.craftbound.network.CraftboundNetwork;
import com.magu1436.craftbound.occupations.adventurer.AdventurerConfig;
import com.magu1436.craftbound.occupations.adventurer.client.AdventurerMovementPenaltyRules;
import com.magu1436.craftbound.occupations.adventurer.client.AdventurerMovementPenaltyService;
import com.magu1436.craftbound.occupations.adventurer.experience.AdventurerMobKillExperienceSource;
import com.magu1436.craftbound.occupations.adventurer.rewards.AdventurerRewardsFactory;
import com.magu1436.craftbound.occupations.architect.ArchitectConfig;
import com.magu1436.craftbound.occupations.architect.experience.ArchitectConstructionExperienceSource;
import com.magu1436.craftbound.occupations.architect.rewards.ArchitectRewardFactory;
import com.magu1436.craftbound.occupations.blacksmith.reward.BlacksmithRewardsFactory;
import com.magu1436.craftbound.occupations.explorer.integration.PufferfishExplorerExperienceGateway;
import com.magu1436.craftbound.occupations.explorer.rewards.ExplorerRewardsFactory;
import com.magu1436.craftbound.occupations.foodproducer.loot.FoodProducerLootModifiers;
import com.magu1436.craftbound.registry.CraftboundAttributes;
import com.magu1436.craftbound.registry.CraftboundBlockEntities;
import com.magu1436.craftbound.registry.CraftboundBlocks;
import com.magu1436.craftbound.registry.CraftboundItems;
import com.magu1436.craftbound.registry.CraftboundMenus;
import com.magu1436.craftbound.registry.CraftboundRecipeSerializers;
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

@Mod(Craftbound.MODID)
public class Craftbound {

    public static final String MODID = "craftbound";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Craftbound(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

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

        CraftboundItems.register(modEventBus);
        CraftboundBlocks.register(modEventBus);
        CraftboundBlockEntities.register(modEventBus);
        CraftboundMenus.register(modEventBus);
        CraftboundRecipeSerializers.register(modEventBus);
        CraftboundAttributes.register(modEventBus);
        FoodProducerLootModifiers.register(modEventBus);

        AdventurerMobKillExperienceSource.register();
        PufferfishExplorerExperienceGateway.register();
        ArchitectConstructionExperienceSource.register();

        AdventurerRewardsFactory.registerRewards();
        ArchitectRewardFactory.registerRewards();
        ExplorerRewardsFactory.registerRewards();
        BlacksmithRewardsFactory.registerRewards();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(CraftboundNetwork::register);

        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock) {
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        }

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

            event.enqueueWork(ClientModEvents::registerMovementPenaltyEventHandler);
        }

        private static void registerMovementPenaltyEventHandler() {
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
