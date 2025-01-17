/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import static dev.terminalmc.claimpoints.util.Localization.localized;
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
        int total = ClaimPoints.waypointManager.showClaimPoints();
        sendWithPrefix(localized("message", "waypoint.enabled", total));
        return Command.SINGLE_SUCCESS;
    }

    private static int hideClaimPoints() {
        int total = ClaimPoints.waypointManager.hideClaimPoints();
        sendWithPrefix(localized("message", "waypoint.disabled", total));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearClaimPoints() {
        int removed = ClaimPoints.waypointManager.clearClaimPoints();
        sendWithPrefix(localized("message", "waypoint.removed", removed));
        return Command.SINGLE_SUCCESS;
    }

    private static int setNameFormat(String nameFormat) {
        int indexOfSize = nameFormat.indexOf("%d");
        if (indexOfSize != -1) {
            ClaimPoints.waypointManager.setClaimPointNameFormat(nameFormat);
            Config.cpSettings().nameFormat = nameFormat;
            Config.cpSettings().namePattern = "^" + Pattern.quote(nameFormat.substring(0, indexOfSize)) +
                    "(\\d+)" + Pattern.quote(nameFormat.substring(indexOfSize + 2)) + "$";
            Config.cpSettings().nameCompiled = Pattern.compile(Config.cpSettings().namePattern);
            Config.save();
            sendWithPrefix(localized("message", "waypoint.nameFormat.set", nameFormat));
        }
        else {
            sendWithPrefix(localized("message", "waypoint.nameFormat.error", nameFormat));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int setAlias(String alias) {
        alias = alias.length() <= 2 ? alias : alias.substring(0, 2);
        ClaimPoints.waypointManager.setClaimPointAlias(alias);
        Config.cpSettings().alias = alias;
        Config.save();

        sendWithPrefix(localized("message", "waypoint.alias.set", alias));
        return Command.SINGLE_SUCCESS;
    }

    private static int setColor(String color) {
        int index = ClaimPoints.waypointColorNames.indexOf(color);
        if (index == -1) {
            sendWithPrefix(localized("message", "waypoint.color.error", color));
        }
        else {
            ClaimPoints.waypointManager.setClaimPointColor(index);
            Config.cpSettings().color = color;
            Config.cpSettings().colorIdx = index;
            Config.save();
            sendWithPrefix(localized("message", "waypoint.color.set", color));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int getWorlds() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand(Config.gpSettings().claimListCommand);
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
            connection.sendCommand(Config.gpSettings().claimListCommand);
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
        msg.append(localized("message", "command.help.worlds").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp add <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.add").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp clean <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.clean").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp update <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.update").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints show\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.show").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints hide\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.hide").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints clear\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.clear").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set nameformat <name format>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.set.nameFormat").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set alias <alias>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.set.alias").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set color <color>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(localized("message", "command.help.waypoints.set.color").append("\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("===============================================\n");
        send(msg);
        return Command.SINGLE_SUCCESS;
    }
    
    public static void sendWithPrefix(Component content) {
        MutableComponent message = ClaimPoints.PREFIX.copy();
        message.append(content);
        send(message);
    }

    private static void send(Component message) {
        Minecraft.getInstance().gui.getChat().addMessage(message);
    }
}
