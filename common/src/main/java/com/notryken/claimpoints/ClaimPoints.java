package com.notryken.claimpoints;

import com.notryken.claimpoints.util.MsgScanner;
import com.notryken.claimpoints.util.WaypointManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class ClaimPoints {
    // Constants
    public static final String MOD_ID = "claimpoints";
    public static final String MOD_NAME = "ClaimPoints";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("C").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("P").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.DARK_AQUA);

    public static WaypointManager waypointManager = null;

    public static final Pattern CLAIMPOINT_NAME_PATTERN = Pattern.compile("^Claim \\(\\d+\\)$");

    public static void init() {
        // Config load, eventually
    }

    public static void onEndTick(Minecraft minecraft) {
        MsgScanner.checkStop();
    }

    public static void setWaypointManager(WaypointManager waypointManager) {
        ClaimPoints.waypointManager = waypointManager;
    }
}