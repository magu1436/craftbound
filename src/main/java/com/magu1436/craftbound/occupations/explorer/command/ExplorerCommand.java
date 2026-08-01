package com.magu1436.craftbound.occupations.explorer.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.magu1436.craftbound.occupations.explorer.data.ExplorerDiscoveryData;
import com.magu1436.craftbound.occupations.explorer.discovery.ExplorerDiscoveryManager;
import com.magu1436.craftbound.occupations.explorer.discovery.StructureInstanceKey;
import com.magu1436.craftbound.registry.CraftboundCapabilities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;

/** 発見履歴の確認・調査・リセットを行う管理コマンド。 */
public final class ExplorerCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PAGE_SIZE = 20;

    private ExplorerCommand() {
    }

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(Commands.literal("craftbound")
            .then(Commands.literal("explorer")
                .then(discoveries())
                .then(inspect())
                .then(reset())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> discoveries() {
        LiteralArgumentBuilder<CommandSourceStack> node =
            Commands.literal("discoveries")
                .requires(source -> source.hasPermission(0))
                .executes(context -> summary(
                    context.getSource(), context.getSource().getPlayerOrException()
                ));
        addHistoryTypes(node, false);
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> inspect() {
        LiteralArgumentBuilder<CommandSourceStack> node =
            Commands.literal("inspect")
                .requires(source -> source.hasPermission(2));
        var argument = Commands.argument("player", EntityArgument.player())
            .executes(context -> summary(
                context.getSource(), EntityArgument.getPlayer(context, "player")
            ));
        for (HistoryType type : HistoryType.values()) {
            argument.then(historyNode(type, true));
        }
        node.then(argument);
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reset() {
        LiteralArgumentBuilder<CommandSourceStack> node =
            Commands.literal("reset").requires(source -> source.hasPermission(3));
        var player = Commands.argument("player", EntityArgument.player());
        for (ResetType type : ResetType.values()) {
            LiteralArgumentBuilder<CommandSourceStack> typeNode =
                Commands.literal(type.commandName);
            if (type == ResetType.ALL) {
                typeNode.then(Commands.literal("confirm")
                    .executes(context -> reset(context, type)));
            } else {
                typeNode.executes(context -> reset(context, type));
            }
            player.then(typeNode);
        }
        node.then(player);
        return node;
    }

    private static void addHistoryTypes(
        LiteralArgumentBuilder<CommandSourceStack> parent,
        boolean inspected
    ) {
        for (HistoryType type : HistoryType.values()) {
            parent.then(historyNode(type, inspected));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> historyNode(
        HistoryType type,
        boolean inspected
    ) {
        return Commands.literal(type.commandName)
            .executes(context -> history(context, type, inspected, 1))
            .then(Commands.argument("page", IntegerArgumentType.integer(1))
                .executes(context -> history(
                    context, type, inspected,
                    IntegerArgumentType.getInteger(context, "page")
                )));
    }

    private static int history(
        CommandContext<CommandSourceStack> context,
        HistoryType type,
        boolean inspected,
        int page
    ) throws CommandSyntaxException {
        ServerPlayer player = inspected
            ? EntityArgument.getPlayer(context, "player")
            : context.getSource().getPlayerOrException();
        ExplorerDiscoveryData data = data(player);
        if (data == null) {
            context.getSource().sendFailure(Component.literal(
                "Explorer discovery data is unavailable."
            ));
            return 0;
        }
        List<String> entries = type.entries(data);
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > pages) {
            context.getSource().sendFailure(Component.literal(
                "Page " + page + " does not exist (max " + pages + ")."
            ));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal(
            player.getGameProfile().getName() + " " + type.commandName
                + " (" + entries.size() + ", page " + page + "/" + pages + ")"
        ), false);
        int from = (page - 1) * PAGE_SIZE;
        entries.subList(from, Math.min(from + PAGE_SIZE, entries.size()))
            .forEach(entry -> context.getSource().sendSuccess(
                () -> Component.literal("- " + entry), false
            ));
        return entries.size();
    }

    private static int summary(
        CommandSourceStack source,
        ServerPlayer player
    ) {
        ExplorerDiscoveryData data = data(player);
        if (data == null) {
            source.sendFailure(Component.literal(
                "Explorer discovery data is unavailable."
            ));
            return 0;
        }
        int structures = structureCount(data);
        source.sendSuccess(() -> Component.literal(
            player.getGameProfile().getName() + " discoveries: biomes="
                + data.discoveredBiomes().size() + ", structures="
                + structures + ", dimensions="
                + data.discoveredDimensions().size()
        ), false);
        return data.discoveredBiomes().size() + structures
            + data.discoveredDimensions().size();
    }

    private static int reset(
        CommandContext<CommandSourceStack> context,
        ResetType type
    ) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        ExplorerDiscoveryData data = data(player);
        if (data == null) {
            context.getSource().sendFailure(Component.literal(
                "Explorer discovery data is unavailable."
            ));
            return 0;
        }
        int removed = type.remove(data);
        ExplorerDiscoveryManager.instance().clear(player.getUUID());
        CommandSourceStack source = context.getSource();
        LOGGER.info(
            "Explorer reset audit timestamp={} executor={} target_uuid={} type={} removed={}",
            Instant.now(), source.getTextName(), player.getUUID(),
            type.commandName, removed
        );
        source.sendSuccess(() -> Component.literal(
            "Reset " + type.commandName + " discoveries for "
                + player.getGameProfile().getName() + " (removed "
                + removed + ")."
        ), true);
        return removed;
    }

    private static ExplorerDiscoveryData data(ServerPlayer player) {
        return player.getCapability(CraftboundCapabilities.EXPLORER_DISCOVERY_DATA)
            .resolve().orElse(null);
    }

    private static int structureCount(ExplorerDiscoveryData data) {
        return data.discoveredStructures().values().stream()
            .mapToInt(Set::size).sum();
    }

    private enum HistoryType {
        BIOMES("biomes"), STRUCTURES("structures"), DIMENSIONS("dimensions");

        private final String commandName;

        HistoryType(String commandName) {
            this.commandName = commandName;
        }

        List<String> entries(ExplorerDiscoveryData data) {
            List<String> result = new ArrayList<>();
            if (this == BIOMES) {
                result.addAll(data.discoveredBiomes());
            } else if (this == DIMENSIONS) {
                result.addAll(data.discoveredDimensions());
            } else {
                data.discoveredStructures().forEach((id, keys) -> keys.forEach(
                    key -> result.add(id + " @ " + key.dimensionId()
                        + " [" + key.startChunkX() + ","
                        + key.startChunkZ() + "]")
                ));
            }
            result.sort(String::compareTo);
            return result;
        }
    }

    private enum ResetType {
        BIOMES("biomes"), STRUCTURES("structures"),
        DIMENSIONS("dimensions"), ALL("all");

        private final String commandName;

        ResetType(String commandName) {
            this.commandName = commandName;
        }

        int remove(ExplorerDiscoveryData data) {
            Set<String> biomes = data.discoveredBiomes();
            Map<String, Set<StructureInstanceKey>> structures =
                data.discoveredStructures();
            Set<String> dimensions = data.discoveredDimensions();
            int removed = this == BIOMES ? biomes.size()
                : this == STRUCTURES ? structureCount(data)
                : this == DIMENSIONS ? dimensions.size()
                : biomes.size() + structureCount(data) + dimensions.size();
            data.replaceWith(
                this == BIOMES || this == ALL ? Set.of() : biomes,
                this == STRUCTURES || this == ALL ? Map.of()
                    : new HashMap<>(structures),
                this == DIMENSIONS || this == ALL ? Set.of() : dimensions
            );
            return removed;
        }
    }
}
