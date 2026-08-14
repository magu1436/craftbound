package com.magu1436.craftbound.occupations.blacksmith.command;

import java.util.Optional;
import java.util.UUID;

import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinition;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitionSnapshot;
import com.magu1436.craftbound.occupations.blacksmith.casting.definition.MetalPartDefinitions;
import com.magu1436.craftbound.occupations.blacksmith.casting.part.RoughMetalPartStateService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** 鍛造工程の動作確認に使用する状態付き粗加工パーツを生成する。 */
public final class BlacksmithTestCommand {
    private static final int TEST_HEATING_SCORE = 100;

    private BlacksmithTestCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("craftbound")
            .then(Commands.literal("blacksmith")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give_test_rough_part")
                    .then(Commands.argument("definition", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                            MetalPartDefinitions.INSTANCE.ids(), builder
                        ))
                        .executes(BlacksmithTestCommand::giveRoughPart)))));
    }

    private static int giveRoughPart(
        CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ResourceLocation definitionId = ResourceLocationArgument.getId(
            context, "definition"
        );
        Optional<MetalPartDefinition> definitionResult =
            MetalPartDefinitions.INSTANCE.get(definitionId);
        if (definitionResult.isEmpty()) {
            source.sendFailure(Component.literal(
                "Unknown metal part definition: " + definitionId
            ));
            return 0;
        }

        MetalPartDefinitionSnapshot snapshot = definitionResult.get().snapshot();
        Optional<ItemStack> stackResult = RoughMetalPartStateService.create(
            UUID.randomUUID(),
            player.getUUID(),
            snapshot,
            0L,
            TEST_HEATING_SCORE,
            snapshot.cooling().safeTicks(),
            snapshot.forging().breakOnHit()
        );
        if (stackResult.isEmpty()) {
            source.sendFailure(Component.literal(
                "Failed to create a rough metal part for: " + definitionId
            ));
            return 0;
        }

        ItemStack stack = stackResult.get();
        Component itemName = stack.getHoverName().copy();
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.literal("Gave test rough metal part: ")
            .append(itemName)
            .append(" (" + definitionId + ")"), true);
        return 1;
    }
}
