package com.notryken.claimpoints;

import com.notryken.claimpoints.config.Config;
import com.notryken.claimpoints.util.ModLogger;
import com.notryken.claimpoints.util.MsgScanner;
import com.notryken.claimpoints.util.WaypointManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

import static com.notryken.claimpoints.util.Localization.translationKey;

public class ClaimPoints {
    public static final String MOD_ID = "claimpoints";
    public static final String MOD_NAME = "ClaimPoints";
    public static final String COMMAND_ALIAS = "cp";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("C").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("P").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);

    public static WaypointManager waypointManager = null;
    public static List<String> waypointColorNames = null;

    public static void init() {
        waypointManager = new WaypointManager();
        waypointColorNames = waypointManager.getColorNames();
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
        MsgScanner.checkStop();
    }

    public static void onConfigSaved(Config config) {
        // If you are maintaining caches based on config values, update them here.
    }
}
