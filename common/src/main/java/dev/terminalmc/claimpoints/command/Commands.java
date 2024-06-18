package dev.terminalmc.claimpoints.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.config.Config;
import dev.terminalmc.claimpoints.util.ChatScanner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@SuppressWarnings("unchecked")
public class Commands<S> extends CommandDispatcher<S> {
    public void register(Minecraft mc, CommandDispatcher<S> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register((LiteralArgumentBuilder<S>)literal(ClaimPoints.COMMAND_ALIAS)
                .then(literal("help")
                        .executes(ctx -> showHelp()))
                .then(literal("waypoints")
                        .then(literal("show")
                                .executes(ctx -> showClaimPoints()))
                        .then(literal("hide")
                                .executes(ctx -> hideClaimPoints()))
                        .then(literal("clear")
                                .executes(ctx -> clearClaimPoints()))
                        .then(literal("set")
                                .then(literal("nameformat")
                                        .then(argument("name format", StringArgumentType.greedyString())
                                                .executes(ctx -> setNameFormat(StringArgumentType.getString(ctx, "name format")))))
                                .then(literal("alias")
                                        .then(argument("alias", StringArgumentType.greedyString())
                                                .executes(ctx -> setAlias(StringArgumentType.getString(ctx, "alias")))))
                                .then(literal("color")
                                        .then(argument("color", StringArgumentType.greedyString())
                                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(ClaimPoints.waypointColorNames, builder)))
                                                .executes(ctx -> setColor(StringArgumentType.getString(ctx, "color")))))))
                .then(literal("worlds")
                        .executes(ctx -> getWorlds()))
                .then(literal("add")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(ChatScanner.getWorlds(), builder)))
                                .executes(ctx -> addFrom(StringArgumentType.getString(ctx, "world name")))))
                .then(literal("clean")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(ChatScanner.getWorlds(), builder)))
                                .executes(ctx -> cleanFrom(StringArgumentType.getString(ctx, "world name")))))
                .then(literal("update")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(ChatScanner.getWorlds(), builder)))
                                .executes(ctx -> updateFrom(StringArgumentType.getString(ctx, "world name"))))));
    }

    private static int showClaimPoints() {
        ClaimPoints.waypointManager.showClaimPoints();
        sendWithPrefix("Enabled all ClaimPoints");
        return Command.SINGLE_SUCCESS;
    }

    private static int hideClaimPoints() {
        ClaimPoints.waypointManager.hideClaimPoints();
        sendWithPrefix("Disabled all ClaimPoints");
        return Command.SINGLE_SUCCESS;
    }

    private static int clearClaimPoints() {
        int removed = ClaimPoints.waypointManager.clearClaimPoints();
        sendWithPrefix("Removed all ClaimPoints (" + removed + ").");
        return Command.SINGLE_SUCCESS;
    }

    private static int setNameFormat(String nameFormat) {
        int indexOfSize = nameFormat.indexOf("%d");
        if (indexOfSize != -1) {
            ClaimPoints.waypointManager.setClaimPointNameFormat(nameFormat);
            Config.get().cpSettings.nameFormat = nameFormat;
            Config.get().cpSettings.namePattern = "^" + Pattern.quote(nameFormat.substring(0, indexOfSize)) +
                    "(\\d+)" + Pattern.quote(nameFormat.substring(indexOfSize + 2)) + "$";
            Config.get().cpSettings.nameCompiled = Pattern.compile(Config.get().cpSettings.namePattern);
            Config.save();
            sendWithPrefix("Set ClaimPoint name format to '" + nameFormat + "'.");
        }
        else {
            sendWithPrefix("'" + nameFormat + "' is not a valid name format. Requires %d for claim size.");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setAlias(String alias) {
        alias = alias.length() <= 2 ? alias : alias.substring(0, 2);
        ClaimPoints.waypointManager.setClaimPointAlias(alias);
        Config.get().cpSettings.alias = alias;
        Config.save();

        sendWithPrefix("Set alias of all ClaimPoints to " + alias);
        return Command.SINGLE_SUCCESS;
    }

    private static int setColor(String color) {
        int index = ClaimPoints.waypointColorNames.indexOf(color);
        if (index == -1) {
            sendWithPrefix("'" + color + "' is not a valid color ID.");
        }
        else {
            ClaimPoints.waypointManager.setClaimPointColor(index);
            Config.get().cpSettings.color = color;
            Config.get().cpSettings.colorIdx = index;
            Config.save();
            sendWithPrefix("Set color of all ClaimPoints to " + color);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int getWorlds() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand(Config.get().gpSettings.claimListCommand);
            ChatScanner.startWorldScan();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addFrom(String world) {
        return scanFrom(world, ChatScanner.ScanType.ADD);
    }

    private static int cleanFrom(String world) {
        return scanFrom(world, ChatScanner.ScanType.CLEAN);
    }

    private static int updateFrom(String world) {
        return scanFrom(world, ChatScanner.ScanType.UPDATE);
    }

    private static int scanFrom(String world, ChatScanner.ScanType scanType) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("claimlist");
            ChatScanner.startClaimScan(world, scanType);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showHelp() {
        MutableComponent msg = Component.empty().withStyle(ChatFormatting.DARK_GRAY);
        msg.append("\n============= ");
        msg.append(Component.literal("Claim").withStyle(ChatFormatting.AQUA));
        msg.append(Component.literal("Points").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(" by ");
        msg.append(Component.literal("NotRyken").withStyle(ChatFormatting.GRAY));
        msg.append(" =============\n");
        msg.append(Component.literal("/cp worlds\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Lists the GriefPrevention worlds in which you have active claims, and " +
                        "stores them for future autocompletion.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp add <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Adds the northwest-corner coordinate of all claims in the specified " +
                        "GriefPrevention world as Xaero's Minimap waypoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp clean <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Removes all ClaimPoints in the active waypoint list, " +
                        "that do not match a claim in the specified world.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp update <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Combines /cp add <world> and /cp clean <world>, and also updates " +
                        "ClaimPoint size indicators.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints show\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Enables (shows) all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints hide\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Disables (hides) all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints clear\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Permanently deletes all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set nameformat <name format>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the name format of all ClaimPoints to the specified value. " +
                        "Note: the name format must contain %d.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set alias <alias>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the alias (symbol) of all ClaimPoints to the specified value.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set color <color>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the color of all ClaimPoints to the specified value.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("===============================================\n");
        send(msg);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendWithPrefix(String content) {
        MutableComponent message = ClaimPoints.PREFIX.copy();
        message.append(content);
        send(message);
    }

    private static void send(Component message) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(message);
        }
    }
}
