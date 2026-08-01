package com.magu1436.craftbound.testing;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** OP権限を持つプレイテスター向けの状態準備・観測コマンド。 */
@Mod.EventBusSubscriber(modid = Craftbound.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CraftboundTestCommands {

    private CraftboundTestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> qualitySet = Commands.literal("set");
        qualitySet.then(qualityLiteral("high", FoodQuality.HIGH));
        qualitySet.then(qualityLiteral("standard", FoodQuality.STANDARD));
        qualitySet.then(qualityLiteral("low", FoodQuality.LOW));
        qualitySet.then(qualityLiteral("spoiled", FoodQuality.SPOILED));

        event.getDispatcher().register(Commands.literal("craftbound")
                .then(Commands.literal("test")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("quality")
                                .then(qualitySet)
                                .then(Commands.literal("get")
                                        .executes(context -> showHeldQuality(context.getSource())))
                                .then(Commands.literal("resume")
                                        .executes(context -> resumeHeldQuality(context.getSource()))))
                        .then(Commands.literal("food")
                                .then(Commands.literal("get")
                                        .executes(context -> showFoodStatus(context.getSource())))
                                .then(Commands.literal("reset")
                                        .executes(context -> setFoodStatus(context.getSource(), 10, 0.0F)))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("hunger", IntegerArgumentType.integer(0, 20))
                                                .then(Commands.argument(
                                                                "saturation",
                                                                FloatArgumentType.floatArg(0.0F, 20.0F)
                                                        )
                                                        .executes(context -> setFoodStatus(
                                                                context.getSource(),
                                                                IntegerArgumentType.getInteger(context, "hunger"),
                                                                FloatArgumentType.getFloat(context, "saturation")
                                                        ))))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> qualityLiteral(
            String name,
            FoodQuality quality
    ) {
        return Commands.literal(name)
                .executes(context -> setHeldQuality(context.getSource(), quality));
    }

    private static int setHeldQuality(CommandSourceStack source, FoodQuality quality)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !FoodQualityItems.isQualityTarget(stack)) {
            source.sendFailure(Component.translatable("command.craftbound.test.quality.invalid_item"));
            return 0;
        }

        long gameTime = player.serverLevel().getGameTime();
        FoodQualityData.initialize(stack, quality, gameTime);
        FoodQualityData.freezeForTesting(stack, gameTime);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.quality.set",
                stack.getHoverName(),
                qualityName(quality)
        ), false);
        return 1;
    }

    private static int showHeldQuality(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        FoodQuality quality = FoodQualityData.get(stack).orElse(null);
        if (quality == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.quality.untracked"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.quality.get",
                stack.getHoverName(),
                qualityName(quality),
                Component.translatable(FoodQualityData.isFrozenForTesting(stack)
                        ? "command.craftbound.test.yes"
                        : "command.craftbound.test.no")
        ), false);
        return 1;
    }

    private static int resumeHeldQuality(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!FoodQualityData.hasQuality(stack)) {
            source.sendFailure(Component.translatable("command.craftbound.test.quality.untracked"));
            return 0;
        }
        FoodQualityData.unfreezeForTesting(stack, player.serverLevel().getGameTime());
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.quality.resumed",
                stack.getHoverName()
        ), false);
        return 1;
    }

    private static int showFoodStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FoodData food = player.getFoodData();
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.food.get",
                food.getFoodLevel(),
                formatFloat(food.getSaturationLevel())
        ), false);
        return food.getFoodLevel();
    }

    private static int setFoodStatus(CommandSourceStack source, int hunger, float saturation)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        float appliedSaturation = Math.min(hunger, saturation);
        FoodData food = player.getFoodData();
        food.setFoodLevel(hunger);
        food.setSaturation(appliedSaturation);
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.food.set",
                hunger,
                formatFloat(appliedSaturation)
        ), false);
        return hunger;
    }

    private static Component qualityName(FoodQuality quality) {
        return Component.translatable(switch (quality) {
            case HIGH -> "quality.craftbound.high";
            case STANDARD -> "quality.craftbound.standard";
            case LOW -> "quality.craftbound.low";
            case SPOILED -> "quality.craftbound.spoiled";
        });
    }

    private static String formatFloat(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
