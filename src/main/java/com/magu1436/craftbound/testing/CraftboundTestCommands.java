package com.magu1436.craftbound.testing;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.magu1436.craftbound.Craftbound;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingData;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingRecipeManager;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodCookingTestHooks;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQuality;
import com.magu1436.craftbound.occupations.foodproducer.processing.FoodProcessingBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityData;
import com.magu1436.craftbound.occupations.foodproducer.quality.FoodQualityItems;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchAnimalData;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchBlockEntity;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchManagementEvents;
import com.magu1436.craftbound.occupations.foodproducer.ranch.RanchManager;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerPendingExperience;
import com.magu1436.craftbound.occupations.foodproducer.skills.FoodProducerSkills;
import com.magu1436.craftbound.occupations.foodproducer.storage.PreservationStorageBlockEntity;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Experience;
import net.puffish.skillsmod.api.SkillsAPI;

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

        LiteralArgumentBuilder<CommandSourceStack> test = Commands.literal("test")
                .requires(source -> source.hasPermission(2));

        test.then(Commands.literal("quality")
                .then(qualitySet)
                .then(Commands.literal("get")
                        .executes(context -> showHeldQuality(context.getSource())))
                .then(Commands.literal("resume")
                        .executes(context -> resumeHeldQuality(context.getSource())))
                .then(Commands.literal("countdown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(context -> setHeldQualityCountdown(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")
                                )))));

        test.then(Commands.literal("food")
                .then(Commands.literal("get")
                        .executes(context -> showFoodStatus(context.getSource())))
                .then(Commands.literal("reset")
                        .executes(context -> setFoodStatus(context.getSource(), 10, 0.0F)))
                .then(Commands.literal("set")
                        .then(Commands.argument("hunger", IntegerArgumentType.integer(0, 20))
                                .then(Commands.argument("saturation", FloatArgumentType.floatArg(0.0F, 20.0F))
                                        .executes(context -> setFoodStatus(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "hunger"),
                                                FloatArgumentType.getFloat(context, "saturation")
                                        ))))));

        test.then(Commands.literal("ranch")
                .then(Commands.literal("status")
                        .executes(context -> showNearestRanchAnimalStatus(context.getSource())))
                .then(Commands.literal("feed_due")
                        .executes(context -> setNearestFeedDue(context.getSource())))
                .then(Commands.literal("feed_now")
                        .executes(context -> feedNearestNow(context.getSource())))
                .then(Commands.literal("child_grow")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 3600))
                                .executes(context -> prepareNearestChildGrowth(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "seconds")
                                ))))
                .then(Commands.literal("child_pause")
                        .executes(context -> pauseNearestChild(context.getSource())))
                .then(Commands.literal("child_resume")
                        .executes(context -> resumeNearestChild(context.getSource())))
                .then(Commands.literal("breeding_ready")
                        .executes(context -> prepareNearestBreedingPair(context.getSource())))
                .then(Commands.literal("force_extra")
                        .executes(context -> forceNextExtraChild(context.getSource()))));

        test.then(Commands.literal("experience")
                .then(Commands.literal("get")
                        .executes(context -> showFoodProducerExperience(context.getSource())))
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(context -> addFoodProducerExperience(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount")
                                ))))
                .then(Commands.literal("pending")
                        .executes(context -> showPendingFoodProducerExperience(context.getSource())))
                .then(Commands.literal("pending_add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(context -> addPendingFoodProducerExperience(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount")
                                ))))
                .then(Commands.literal("deliver")
                        .executes(context -> deliverPendingFoodProducerExperience(context.getSource()))));

        test.then(Commands.literal("processing")
                .then(Commands.literal("status")
                        .executes(context -> showNearestProcessingStatus(context.getSource())))
                .then(Commands.literal("create_start")
                        .executes(context -> startNearestCreateAutomation(context.getSource())))
                .then(Commands.literal("finish")
                        .executes(context -> finishNearestProcessing(context.getSource()))));

        test.then(Commands.literal("storage")
                .then(Commands.literal("status")
                        .executes(context -> showNearestStorageStatus(context.getSource()))));

        LiteralArgumentBuilder<CommandSourceStack> cookingPrepare = Commands.literal("prepare");
        cookingPrepare.then(cookingQualityLiteral("high", FoodQuality.HIGH));
        cookingPrepare.then(cookingQualityLiteral("standard", FoodQuality.STANDARD));
        cookingPrepare.then(cookingQualityLiteral("low", FoodQuality.LOW));
        test.then(Commands.literal("cooking")
                .then(cookingPrepare)
                .then(Commands.literal("ingredients")
                        .then(Commands.argument("recipe", ResourceLocationArgument.id())
                                .executes(context -> giveTestCookingIngredients(
                                        context.getSource(),
                                        ResourceLocationArgument.getId(context, "recipe")
                                ))))
                .then(Commands.literal("force_upgrade")
                        .executes(context -> forceNextCookingUpgrade(context.getSource()))));

        event.getDispatcher().register(Commands.literal("craftbound").then(test));
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
        FoodQualityData.Snapshot snapshot = FoodQualityData.snapshot(
                stack,
                player.serverLevel().getGameTime()
        ).orElse(null);
        if (snapshot == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.quality.untracked"));
            return 0;
        }
        FoodQuality quality = snapshot.quality();
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

    private static int showNearestRanchAnimalStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, ignored -> true);
        if (animal == null) return 0;
        long now = animal.level().getGameTime();
        long nextFeed = RanchAnimalData.hasNextFeedTime(animal)
                ? Math.max(0L, RanchAnimalData.getNextFeedTime(animal) - now) / 20L
                : -1L;
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.status",
                animal.getDisplayName(),
                RanchAnimalData.isFed(animal),
                animal.isBaby() ? RanchAnimalData.getRemainingGrowth(animal) / 20 : 0,
                nextFeed
        ), false);
        return 1;
    }

    private static int setNearestFeedDue(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, ignored -> true);
        if (animal == null) return 0;
        RanchAnimalData.setFed(animal, false);
        RanchAnimalData.setNextFeedTime(animal, animal.level().getGameTime());
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.feed_due",
                animal.getDisplayName()
        ), false);
        return 1;
    }

    private static int feedNearestNow(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, ignored -> true);
        if (animal == null) return 0;
        RanchAnimalData.setFed(animal, true);
        RanchAnimalData.setNextFeedTime(
                animal,
                animal.level().getGameTime() + (animal.isBaby()
                        ? RanchAnimalData.CHILD_FEED_INTERVAL_TICKS
                        : RanchAnimalData.ADULT_FEED_INTERVAL_TICKS)
        );
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.feed_now",
                animal.getDisplayName()
        ), false);
        return 1;
    }

    private static int prepareNearestChildGrowth(CommandSourceStack source, int seconds)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, Animal::isBaby);
        if (animal == null) return 0;
        RanchAnimalData.ensureManaged(animal);
        RanchAnimalData.setRemainingGrowth(animal, seconds * 20);
        RanchAnimalData.setFed(animal, true);
        RanchAnimalData.setNextFeedTime(
                animal,
                animal.level().getGameTime() + RanchAnimalData.CHILD_FEED_INTERVAL_TICKS
        );
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.child_remaining",
                animal.getDisplayName(),
                seconds
        ), false);
        return 1;
    }

    private static int pauseNearestChild(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, Animal::isBaby);
        if (animal == null) return 0;
        RanchAnimalData.ensureManaged(animal);
        RanchAnimalData.setFed(animal, false);
        RanchAnimalData.setNextFeedTime(animal, animal.level().getGameTime() + 60L * 60L * 20L);
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.child_paused",
                animal.getDisplayName()
        ), false);
        return 1;
    }

    private static int resumeNearestChild(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Animal animal = nearestAnimal(source, Animal::isBaby);
        if (animal == null) return 0;
        RanchAnimalData.ensureManaged(animal);
        RanchAnimalData.setFed(animal, true);
        RanchAnimalData.setNextFeedTime(
                animal,
                animal.level().getGameTime() + RanchAnimalData.CHILD_FEED_INTERVAL_TICKS
        );
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.child_resumed",
                animal.getDisplayName()
        ), false);
        return 1;
    }

    private static int prepareNearestBreedingPair(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Animal nearest = nearestAnimal(source, animal -> !animal.isBaby());
        if (nearest == null) return 0;
        RanchBlockEntity ranch = RanchManager.findManagingRanch(nearest);
        if (ranch == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.ranch.no_ranch"));
            return 0;
        }
        List<Animal> pair = RanchManager.getManagedAnimals(ranch).stream()
                .filter(animal -> !animal.isBaby())
                .sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(2)
                .toList();
        if (pair.size() < 2 || RanchManager.getManagedAnimals(ranch).size() >= ranch.getManagementCapacity()) {
            source.sendFailure(Component.translatable("command.craftbound.test.ranch.no_pair_or_capacity"));
            return 0;
        }
        long nextFeed = player.serverLevel().getGameTime() + RanchAnimalData.ADULT_FEED_INTERVAL_TICKS;
        pair.forEach(animal -> {
            animal.setAge(0);
            animal.resetLove();
            RanchAnimalData.setFed(animal, true);
            RanchAnimalData.setNextFeedTime(animal, nextFeed);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.breeding_ready",
                pair.get(0).getDisplayName(),
                pair.get(1).getDisplayName()
        ), false);
        return 1;
    }

    private static int forceNextExtraChild(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        RanchManagementEvents.forceNextExtraChild(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.ranch.force_extra"
        ), false);
        return 1;
    }

    private static int showFoodProducerExperience(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Category category = SkillsAPI.getCategory(FoodProducerSkills.CATEGORY_ID).orElse(null);
        Experience experience = category == null ? null : category.getExperience().orElse(null);
        if (category == null || experience == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.experience.unavailable"));
            return 0;
        }
        int level = experience.getLevel(player);
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.experience.get",
                experience.getTotal(player),
                level,
                experience.getCurrent(player),
                experience.getRequired(player, level),
                category.getPointsLeft(player)
        ), false);
        return experience.getTotal(player);
    }

    private static int addFoodProducerExperience(CommandSourceStack source, int amount)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!FoodProducerExperience.add(source.getPlayerOrException(), amount)) {
            source.sendFailure(Component.translatable("command.craftbound.test.experience.unavailable"));
            return 0;
        }
        return showFoodProducerExperience(source);
    }

    private static int showPendingFoodProducerExperience(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int amount = FoodProducerPendingExperience.get(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.experience.pending",
                amount
        ), false);
        return amount;
    }

    private static int addPendingFoodProducerExperience(CommandSourceStack source, int amount)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int total = FoodProducerPendingExperience.queue(source.getServer(), player.getUUID(), amount);
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.experience.pending_add",
                amount,
                total
        ), false);
        return total;
    }

    private static int deliverPendingFoodProducerExperience(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        int delivered = FoodProducerPendingExperience.deliver(source.getPlayerOrException());
        if (delivered < 0) {
            source.sendFailure(Component.translatable("command.craftbound.test.experience.unavailable"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.experience.deliver",
                delivered
        ), false);
        return delivered;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> cookingQualityLiteral(
            String name,
            FoodQuality quality
    ) {
        return Commands.literal(name)
                .executes(context -> giveTestPreparedSet(context.getSource(), quality));
    }

    private static int showNearestProcessingStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoodProcessingBlockEntity processor = nearestProcessing(source);
        if (processor == null) return 0;
        BlockPos pos = processor.getBlockPos();
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.processing.status",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                processor.station().serializedName(),
                processor.currentOperation().serializedName(),
                processor.isRunning(),
                processor.isAutomated(),
                processor.progress(),
                processor.totalTicks(),
                processor.burnTime()
        ), false);
        return processor.isRunning() ? 1 : 0;
    }

    private static int giveTestPreparedSet(CommandSourceStack source, FoodQuality quality)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = FoodCookingData.createTestPreparedSet(
                quality,
                player.serverLevel().getGameTime()
        );
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.cooking.prepare",
                qualityName(quality)
        ), false);
        return 1;
    }

    private static int giveTestCookingIngredients(CommandSourceStack source, ResourceLocation recipeId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var recipe = FoodCookingRecipeManager.get(recipeId).orElse(null);
        if (recipe == null) {
            source.sendFailure(Component.translatable(
                    "command.craftbound.test.cooking.ingredients.unknown", recipeId.toString()
            ));
            return 0;
        }
        List<ItemStack> inputs = recipe.createTestInputs(player.serverLevel().getGameTime());
        if (inputs.stream().anyMatch(ItemStack::isEmpty)) {
            source.sendFailure(Component.translatable(
                    "command.craftbound.test.cooking.ingredients.unavailable", recipeId.toString()
            ));
            return 0;
        }
        for (ItemStack stack : inputs) {
            if (!player.addItem(stack)) player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.cooking.ingredients", recipeId.toString()
        ), false);
        return inputs.size();
    }

    private static int setHeldQualityCountdown(CommandSourceStack source, int seconds)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!FoodQualityData.setRemainingForTesting(
                stack,
                seconds * 20L,
                player.serverLevel().getGameTime()
        )) {
            source.sendFailure(Component.translatable("command.craftbound.test.quality.untracked"));
            return 0;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.quality.countdown",
                stack.getHoverName(),
                seconds
        ), false);
        return 1;
    }

    private static int forceNextCookingUpgrade(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoodCookingTestHooks.forceNextUpgrade(source.getPlayerOrException());
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.cooking.force_upgrade"
        ), false);
        return 1;
    }

    private static int finishNearestProcessing(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoodProcessingBlockEntity processor = nearestProcessing(source);
        if (processor == null) return 0;
        if (!processor.finishForTesting()) {
            source.sendFailure(Component.translatable("command.craftbound.test.processing.not_running"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.craftbound.test.processing.finish"), false);
        return 1;
    }

    private static int startNearestCreateAutomation(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FoodProcessingBlockEntity processor = nearestProcessing(source);
        if (processor == null) return 0;
        if (!processor.startCreateAutomation()) {
            source.sendFailure(Component.translatable(
                    "command.craftbound.test.processing.create_rejected"
            ));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.processing.create_started"
        ), false);
        return 1;
    }

    private static int showNearestStorageStatus(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PreservationStorageBlockEntity storage = nearestStorage(source);
        if (storage == null) return 0;

        int occupiedSlots = 0;
        int totalItems = 0;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack stack = storage.getItem(slot);
            if (!stack.isEmpty()) {
                occupiedSlots++;
                totalItems += stack.getCount();
            }
        }
        BlockPos pos = storage.getBlockPos();
        int finalOccupiedSlots = occupiedSlots;
        int finalTotalItems = totalItems;
        source.sendSuccess(() -> Component.translatable(
                "command.craftbound.test.storage.status",
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                storage.getDisplayName(),
                (int) storage.preservationMultiplier(),
                finalOccupiedSlots,
                PreservationStorageBlockEntity.CONTAINER_SIZE,
                finalTotalItems
        ), false);
        return 1;
    }

    @Nullable
    private static PreservationStorageBlockEntity nearestStorage(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(16.0D, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit
                && player.serverLevel().getBlockEntity(blockHit.getBlockPos())
                        instanceof PreservationStorageBlockEntity lookedAt) {
            return lookedAt;
        }
        BlockPos center = player.blockPosition();
        PreservationStorageBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-16, -16, -16),
                center.offset(16, 16, 16)
        )) {
            if (player.serverLevel().getBlockEntity(pos)
                    instanceof PreservationStorageBlockEntity candidate) {
                double distance = pos.distSqr(center);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.storage.none"));
        }
        return nearest;
    }

    @Nullable
    private static FoodProcessingBlockEntity nearestProcessing(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(16.0D, 1.0F, false);
        if (hit instanceof BlockHitResult blockHit
                && player.serverLevel().getBlockEntity(blockHit.getBlockPos())
                        instanceof FoodProcessingBlockEntity lookedAt) {
            return lookedAt;
        }
        BlockPos center = player.blockPosition();
        FoodProcessingBlockEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -16, -16), center.offset(16, 16, 16))) {
            if (player.serverLevel().getBlockEntity(pos) instanceof FoodProcessingBlockEntity candidate) {
                double distance = pos.distSqr(center);
                if (distance < nearestDistance) {
                    nearest = candidate;
                    nearestDistance = distance;
                }
            }
        }
        if (nearest == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.processing.none"));
        }
        return nearest;
    }

    private static Animal nearestAnimal(CommandSourceStack source, Predicate<Animal> filter)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Animal animal = player.serverLevel().getEntitiesOfClass(
                        Animal.class,
                        player.getBoundingBox().inflate(16.0D),
                        candidate -> candidate.isAlive() && filter.test(candidate)
                ).stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
        if (animal == null) {
            source.sendFailure(Component.translatable("command.craftbound.test.ranch.no_nearby_animal"));
        }
        return animal;
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
