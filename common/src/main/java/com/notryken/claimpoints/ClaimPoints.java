package com.notryken.claimpoints;

import com.notryken.claimpoints.config.Config;
import com.notryken.claimpoints.util.ModLogger;
import com.notryken.claimpoints.util.MsgScanner;
import com.notryken.claimpoints.util.WaypointManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class ClaimPoints {
    // Constants
    public static final String MOD_ID = "claimpoints";
    public static final String MOD_NAME = "ClaimPoints";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("C").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("P").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);

    public static WaypointManager waypointManager = null;
    public static List<String> waypointColorNames = null;

    private static Config CONFIG;

    public static void init(WaypointManager pWaypointManager) {
        waypointManager = pWaypointManager;
        waypointColorNames = waypointManager.getColorNames();

        CONFIG = Config.load();
    }

    public static Config config() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }
        return CONFIG;
    }

    public static void restoreDefaultConfig() {
        CONFIG = new Config();
        CONFIG.verifyConfig();
        CONFIG.writeToFile();
    }

    public static void onEndTick(Minecraft minecraft) {
        MsgScanner.checkStop();
    }
}