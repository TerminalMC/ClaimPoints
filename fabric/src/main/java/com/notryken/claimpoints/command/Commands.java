package com.notryken.claimpoints.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.util.MsgScanner;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
        dispatcher.register(literal("cp")
                .then(literal("help")
                        .executes(ctx -> showHelp()))
                .then(literal("waypoints")
                        .then(literal("show")
                                .executes(ctx -> showClaimPoints()))
                        .then(literal("hide")
                                .executes(ctx -> hideClaimPoints()))
                        .then(literal("clear")
                                .executes(ctx -> clearClaimPoints())))
                .then(literal("worlds")
                        .executes(ctx -> getWorlds()))
                .then(literal("add")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .executes(ctx -> addFrom(StringArgumentType.getString(ctx, "world name")))))
                .then(literal("clean")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .executes(ctx -> cleanFrom(StringArgumentType.getString(ctx, "world name"))))));
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
        msg.append(Component.literal("Lists the GriefPrevention worlds in which you have active claims.\n")
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
        msg.append("===============================================\n");
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showClaimPoints() {
        ClaimPoints.waypointManager.showClaimPoints();
        return Command.SINGLE_SUCCESS;
    }

    private static int hideClaimPoints() {
        ClaimPoints.waypointManager.hideClaimPoints();
        return Command.SINGLE_SUCCESS;
    }

    private static int clearClaimPoints() {
        int removed = ClaimPoints.waypointManager.clearClaimPoints();
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        msg.append("Removed all ClaimPoints (" + removed + ").");
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getWorlds() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("claimlist");
            MsgScanner.startWorldScan();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int cleanFrom(String world) {
        return scanFrom(world, false);
    }

    private static int addFrom(String world) {
        return scanFrom(world, true);
    }

    private static int scanFrom(String world, boolean add) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("claimlist");
            MsgScanner.startClaimScan(world, add);
        }
        return Command.SINGLE_SUCCESS;
    }
}
