/*
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.claimpoints.util;

import com.mojang.datafixers.util.Pair;
import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.command.Commands;
import dev.terminalmc.claimpoints.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * ClaimPoints obtains information about claim-worlds and claims by sending
 * GriefPrevention commands, then scanning chat for the result.
 *
 * <p>There are two types of scan, both of which use the claim-list command:
 * A world scan reads the list of claims, and identifies all unique world names.
 * A claim scan reads the list of claims, and parses all claims for which the
 * world name matches the user-provided world name.</p>
 */
public class ChatScanner {
    private enum ScanState {
        WAITING, READING, ENDING
    }
    public enum ScanType {
        ADD, CLEAN, UPDATE
    }

    private static @Nullable String world;
    private static boolean scanning;
    private static ScanState scanState;
    private static long stopTime;

    private static ScanType scanType;
    private static final Set<String> worlds = new HashSet<>();
    private static final List<Pair<Vec2,Integer>> claims = new ArrayList<>();

    public static Stream<String> getWorlds() {
        return worlds.stream();
    }

    /**
     * Initiates a chat scan for claim worlds.
     *
     * <p>To be called immediately after sending the claim list command.</p>
     */
    public static void startWorldScan() {
        stopTime = System.currentTimeMillis() + 5000;
        world = null; // Indicates a world scan
        worlds.clear();
        scanning = true;
        scanState = ScanState.WAITING;
    }

    /**
     * Initiates a chat scan for claims.
     *
     * <p>To be called immediately after sending the claim list command.</p>
     */
    public static void startClaimScan(@NotNull String pWorld, ScanType pScanType) {
        stopTime = System.currentTimeMillis() + 5000;
        world = pWorld; // Scan for pWorld claims
        scanType = pScanType;
        claims.clear();
        scanning = true;
        scanState = ScanState.WAITING;
    }

    /**
     * Scans are given a TTL after each non-final message read, to guard against
     * being left 'hanging' due to a chat anomaly. This method checks the TTL
     * and stops the scan if it has expired.
     */
    public static void checkStop() {
        if (scanning && System.currentTimeMillis() > stopTime) {
            stopScanTimeout();
            if (world == null) {
                handleWorlds();
            } else {
                handleClaims();
            }
        }
    }

    private static void stopScanTimeout() {
        stopScan();
    }

    private static void stopScanInvalid() {
        stopScan();
        Commands.sendWithPrefix("Unrecognized message found while waiting for " +
                "GriefPrevention message. If the claim list appears in chat, you need to adjust " +
                "the regex patterns in ClaimPoints config to capture it.");
    }

    /**
     * Stops the active scan, if any.
     *
     * <p>Note: Scan initiation methods reset all scan parameters, so no need
     * for that here.</p>
     */
    private static void stopScan() {
        scanning = false;
    }

    /**
     * @return true if the string matches any of the patterns, false otherwise.
     */
    private static boolean anyMatches(String content, List<Pattern> patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the message should be hidden, false otherwise.
     */
    public static boolean tryScan(Component message) {
        if (!scanning) return false;
        return scan(message);
    }

    /**
     * @return true if the message should be hidden, false otherwise.
     */
    private static boolean scan(Component message) {
        stopTime = System.currentTimeMillis() + 1000;
        if (world == null) {
            return worldScan(message);
        } else {
            return claimScan(message);
        }
    }

    /**
     * Parses the message as part of a claim world scan.
     * @return true if the message should be hidden, false otherwise.
     */
    private static boolean worldScan(Component message) {
        String content = message.getString();
        switch(scanState) {
            case WAITING -> {
                if (Config.gpSettings().firstLineCompiled.matcher(content).find()) {
                    // Valid first message, start reading
                    scanState = ScanState.READING;
                    return true;
                }
                else {
                    // Unrecognized message, cancel scan
                    stopScanInvalid();
                    return false;
                }
            }
            case READING -> {
                Matcher clMatcher = Config.gpSettings().claimLineCompiled.matcher(content);
                if (clMatcher.find()) {
                    // Valid claim message, add the world
                    worlds.add(clMatcher.group(1));
                    return true;
                }
                else if (anyMatches(content, Config.gpSettings().ignoredLinesCompiled)) {
                    // Recognized non-claim body message, ignore
                    return true;
                }
                else {
                    // Not a body message, process worlds and begin ending scan
                    scanState = ScanState.ENDING;
                    handleWorlds();
                    // Hide the message if it is a valid ending line
                    return anyMatches(content, Config.gpSettings().endingLinesCompiled);
                }
            }
            case ENDING -> {
                if (anyMatches(content, Config.gpSettings().endingLinesCompiled)) {
                    // Valid ending message, hide it
                    return true;
                }
                else {
                    // Unrecognized message, stop scan and do not hide it
                    stopScan();
                    return false;
                }
            }
            default -> {return false;} // Keep IDE happy
        }
    }

    /**
     * Notifies the user about the result of a world scan.
     *
     * <p>To be called immediately after scan completion.</p>
     */
    private static void handleWorlds() {
        if (worlds.isEmpty()) {
            Commands.sendWithPrefix("No worlds found using command '/" +
                    Config.gpSettings().claimListCommand +
                    "'. That might be the wrong command, or you might not have any claims. " +
                    "If the claim list appears in chat, you need to adjust " +
                    "the regex patterns in ClaimPoints config to capture it.");
        } else {
            StringBuilder sb = new StringBuilder("Claim worlds (" + worlds.size() + "):");
            for (String world : worlds) {
                sb.append("\n  ");
                sb.append(world);
            }
            Commands.sendWithPrefix(sb.toString());
        }
    }

    /**
     * Parses the message as part of a claim scan.
     * @return true if the message should be hidden, false otherwise.
     */
    private static boolean claimScan(Component message) {
        String content = message.getString();
        switch(scanState) {
            case WAITING -> {
                if (Config.gpSettings().firstLineCompiled.matcher(content).find()) {
                    // Valid first message, start reading
                    scanState = ScanState.READING;
                    return true;
                }
                else {
                    // Unrecognized message, cancel scan
                    stopScanInvalid();
                    return false;
                }
            }
            case READING -> {
                Matcher clMatcher = Config.gpSettings().claimLineCompiled.matcher(content);
                if (clMatcher.find()) {
                    // Valid claim message, parse the claim
                    if (clMatcher.group(1).equals(world)) {
                        int x = Integer.parseInt(clMatcher.group(2));
                        int z = Integer.parseInt(clMatcher.group(3));
                        int blocks = Integer.parseInt(clMatcher.group(4));
                        claims.add(new Pair<>(new Vec2(x, z), blocks));
                    }
                    return true;
                }
                else if (anyMatches(content, Config.gpSettings().ignoredLinesCompiled)) {
                    // Recognized non-claim body message, ignore
                    return true;
                }
                else {
                    // Not a body message, process claims and begin ending scan
                    scanState = ScanState.ENDING;
                    handleClaims();
                    // Hide the message if it is a valid ending line
                    return anyMatches(content, Config.gpSettings().endingLinesCompiled);
                }
            }
            case ENDING -> {
                if (anyMatches(content, Config.gpSettings().endingLinesCompiled)) {
                    // Valid ending message, hide it
                    return true;
                }
                else {
                    // Unrecognized message, stop scan and do not hide it
                    stopScan();
                    return false;
                }
            }
            default -> {return false;} // Keep IDE happy
        }
    }

    /**
     * Completes the action requested by the player, using the scanned claim
     * list.
     *
     * <p>To be called immediately after scan completion.</p>
     */
    private static void handleClaims() {
        switch(scanType) {
            case ADD -> addClaimPoints();
            case CLEAN -> cleanClaimPoints();
            case UPDATE -> updateClaimPoints();
        }
    }

    /**
     * Adds all claims from the scanned claim list as waypoints (excluding those
     * which already have waypoints) to the active waypoint list, and notifies
     * the user.
     *
     * <p>Does not remove any waypoints.</p>
     *
     * <p>To be called immediately after scan completion.</p>
     */
    private static void addClaimPoints() {
        if (claims.isEmpty()) {
            Commands.sendWithPrefix(Component.empty()
                    .append(Component.literal("No claims found for '" + world + "'. Use "))
                    .append(Component.literal("/cp worlds").withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal(" to list GriefPrevention worlds in which you " +
                            "have active claims.")));
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
            Commands.sendWithPrefix(sb.toString());
        }
        claims.clear();
    }

    /**
     * Removes all claim waypoints not matching claims in the scanned claim list
     * from the active waypoint list, and notifies the user.
     *
     * <p>Does not add any waypoints.</p>
     *
     * <p>To be called immediately after scan completion.</p>
     */
    private static void cleanClaimPoints() {
        int removed = ClaimPoints.waypointManager.cleanClaimPoints(claims);
        StringBuilder sb = new StringBuilder("Removed ");
        sb.append(removed);
        sb.append(removed == 1 ? " ClaimPoint" : " ClaimPoints");
        sb.append(" not matching a claim in '");
        sb.append(world);
        sb.append("' from the active waypoint list.");
        Commands.sendWithPrefix(sb.toString());
        claims.clear();
    }

    /**
     * Synchronizes the claim waypoints in the active waypoint list using the
     * scanned claim list.
     *
     * <p>May add and/or remove waypoints.</p>
     *
     * <p>To be called immediately after scan completion.</p>
     *
     * <p>Note: If the scanned claim list is empty, does nothing. This is
     * intended to guard against accidental deletion of claims due to e.g.
     * incorrect world selection.</p>
     */
    private static void updateClaimPoints() {
        if (claims.isEmpty()) {
            Commands.sendWithPrefix(Component.empty()
                    .append(Component.literal("No claims found for '" + world + "'. Use "))
                    .append(Component.literal("/cp worlds").withStyle(ChatFormatting.DARK_AQUA))
                    .append(Component.literal(" to list GriefPrevention worlds in which you " +
                            "have active claims, or use /cp clean <world> to remove ClaimPoints.")));
        }
        else {
            int[] totals = ClaimPoints.waypointManager.updateClaimPoints(claims);
            StringBuilder sb = new StringBuilder("Added ");
            sb.append(totals[0]);
            sb.append(totals[0] == 1 ? " new ClaimPoint" : " new ClaimPoints");
            sb.append(" from '");
            sb.append(world);
            sb.append("', updated ");
            sb.append(totals[1]);
            sb.append(totals[1] == 1 ? " ClaimPoint size" : " ClaimPoint sizes");
            sb.append(", and removed ");
            sb.append(totals[2]);
            sb.append(totals[2] == 1 ? " stray ClaimPoint" : " stray ClaimPoints");
            sb.append(" from the active waypoint list.");
            Commands.sendWithPrefix(sb.toString());
        }
        claims.clear();
    }
}
