package com.notryken.claimpoints.util;

import com.mojang.datafixers.util.Pair;
import com.notryken.claimpoints.ClaimPoints;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MsgScanner {

    // TODO make these all config-definable
    private static final Pattern FIRST_LINE_PATTERN = Pattern.compile("^-?\\d+ blocks from play \\+ -?\\d+ bonus = -?\\d+ total.$");
    private static final List<Pattern> IGNORED_LINE_PATTERNS = new ArrayList<>(List.of(
            Pattern.compile("^Claims:$")
    ));
    private static final Pattern CLAIM_LINE_PATTERN = Pattern.compile("^(.+): x(-?\\d+), z(-?\\d+) \\(-?(\\d+) blocks\\)$");
    private static final List<Pattern> ENDING_LINE_PATTERNS = new ArrayList<>(List.of(
            Pattern.compile("^ = -?\\d* blocks left to spend$")
    ));

    private enum ScanState {
        WAITING,
        READING,
        ENDING,
    }

    private static boolean scanning = false;
    private static boolean worldScan = false;
    private static ScanState scanState = ScanState.WAITING;
    private static long stopTime = Long.MAX_VALUE;

    private static String world;
    private static boolean scanAdd;
    private static final Set<String> worlds = new HashSet<>();
    private static final List<Pair<Vec2,Integer>> claims = new ArrayList<>();

    public static boolean scanning() {
        return scanning;
    }

    public static void checkStop() {
        if (scanning && System.currentTimeMillis() > stopTime) {
            stopScan();
            if (worldScan) handleWorlds();
            else handleClaims();
        }
    }

    public static void startWorldScan() {
        stopTime = Long.MAX_VALUE;
        world = null;
        worlds.clear();
        scanning = true;
        worldScan = true;
        scanState = ScanState.WAITING;
    }

    public static void startClaimScan(@NotNull String pWorld, boolean pScanAdd) {
        stopTime = Long.MAX_VALUE;
        world = pWorld;
        scanAdd = pScanAdd;
        claims.clear();
        scanning = true;
        worldScan = false;
        scanState = ScanState.WAITING;
    }

    public static void stopScan() {
        scanning = false;
    }

    public static boolean anyMatches(String content, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the message should be ignored, false otherwise.
     */
    public static boolean scan(Component message) {
        stopTime = System.currentTimeMillis() + 1000;
        if (world == null) {
            return worldScan(message);
        }
        else {
            return claimScan(message);
        }
    }

    private static boolean worldScan(Component message) {
        String content = message.getString();
        switch(scanState) {
            case WAITING -> {
                if (FIRST_LINE_PATTERN.matcher(content).find()) {
                    scanState = ScanState.READING;
                    return true;
                }
                else {
                    stopScan();
                    return false;
                }
            }
            case READING -> {
                Matcher clMatcher = CLAIM_LINE_PATTERN.matcher(content);
                if (clMatcher.find()) {
                    worlds.add(clMatcher.group(1));
                    return true;
                }
                else if (anyMatches(content, IGNORED_LINE_PATTERNS)) {
                    return true;
                }
                else {
                    scanState = ScanState.ENDING;
                    handleWorlds();
                    return anyMatches(content, ENDING_LINE_PATTERNS);
                }
            }
            case ENDING -> {
                if (anyMatches(content, ENDING_LINE_PATTERNS)) {
                    return true;
                }
                else {
                    stopScan();
                    return false;
                }
            }
            default -> {return false;} // Keep IDE happy
        }
    }

    private static void handleWorlds() {
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        if (worlds.isEmpty()) {
            msg.append(Component.literal("No worlds found using /claimlist. " +
                    "That might be the wrong command, or you might not have any claims."));
        }
        else {
            StringBuilder sb = new StringBuilder("Claim worlds (" + worlds.size() + "):");
            for (String world : worlds) {
                sb.append("\n  ");
                sb.append(world);
            }
            msg.append(Component.literal(sb.toString()));
        }
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        worlds.clear();
    }

    private static boolean claimScan(Component message) {
        String content = message.getString();
        switch(scanState) {
            case WAITING -> {
                if (FIRST_LINE_PATTERN.matcher(content).find()) {
                    scanState = ScanState.READING;
                    return true;
                }
                else {
                    stopScan();
                    return false;
                }
            }
            case READING -> {
                Matcher clMatcher = CLAIM_LINE_PATTERN.matcher(content);
                if (clMatcher.find()) {
                    if (clMatcher.group(1).equals(world)) {
                        int x = Integer.parseInt(clMatcher.group(2));
                        int z = Integer.parseInt(clMatcher.group(3));
                        int blocks = Integer.parseInt(clMatcher.group(4));
                        claims.add(new Pair<>(new Vec2(x, z), blocks));
                    }
                    return true;
                }
                else if (anyMatches(content, IGNORED_LINE_PATTERNS)) {
                    return true;
                }
                else {
                    scanState = ScanState.ENDING;
                    handleClaims();
                    return anyMatches(content, ENDING_LINE_PATTERNS);
                }
            }
            case ENDING -> {
                if (anyMatches(content, ENDING_LINE_PATTERNS)) {
                    return true;
                }
                else {
                    stopScan();
                    return false;
                }
            }
            default -> {return false;} // Keep IDE happy
        }
    }

    private static void handleClaims() {
        if (scanAdd) {
            addClaimPoints();
        }
        else {
            cleanClaimPoints();
        }
    }

    private static void addClaimPoints() {
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        if (claims.isEmpty()) {
            msg.append(Component.literal("No claims found for '" + world + "'."));
        }
        else {
            int added = ClaimPoints.waypointManager.addClaimPoints(claims);
            StringBuilder sb = new StringBuilder("Added ");
            sb.append(added);
            sb.append(added == 1 ? " claim from '" : " claims from '");
            sb.append(world);
            sb.append("' to the active waypoint list.");
            int skipped = claims.size() - added;
            if (skipped > 0) {
                sb.append(" Skipped ");
                sb.append(skipped);
                sb.append(skipped == 1 ? " claim that already has a ClaimPoint." :
                        " claims that already have ClaimPoints.");
            }
            msg.append(sb.toString());
        }
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        claims.clear();
    }

    private static void cleanClaimPoints() {
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        int removed = ClaimPoints.waypointManager.cleanClaimPoints(claims);
        StringBuilder sb = new StringBuilder("Removed ");
        sb.append(removed);
        sb.append(removed == 1 ? " ClaimPoint" : " ClaimPoints");
        sb.append(" not matching a claim in '");
        sb.append(world);
        sb.append("' from the active waypoint list.");
        msg.append(sb.toString());
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        claims.clear();
    }
}
