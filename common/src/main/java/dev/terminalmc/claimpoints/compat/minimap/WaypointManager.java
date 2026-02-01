/*
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.claimpoints.compat.minimap;

import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.compat.Claim;
import dev.terminalmc.claimpoints.config.Config;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.settings.ModSettings;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

public class WaypointManager {

    public List<String> getColorNames() {
        return Arrays.asList(ModSettings.ENCHANT_COLOR_NAMES);
    }

    private MinimapSession getSession() {
        return BuiltInHudModules.MINIMAP.getCurrentSession();
    }

    private MinimapWorld getCurrentWorld(MinimapSession session) {
        return session.getWorldManager().getCurrentWorld();
    }

    private WaypointSet getWaypointSet(MinimapWorld world) {
        return world.getCurrentWaypointSet();
    }

    private List<Waypoint> getWaypoints(MinimapWorld world) {
        Iterable<Waypoint> iterable = world.getCurrentWaypointSet().getWaypoints();
        List<Waypoint> waypoints = new ArrayList<>();
        iterable.forEach(waypoints::add);
        return waypoints;
    }

    /**
     * @return {@code true} if the waypoint name matches the configured claim-point name pattern.
     */
    private boolean isClaimPoint(Waypoint waypoint) {
        return Config.cpSettings().nameCompiled.matcher(waypoint.getName()).find();
    }

    /**
     * @return {@code true} if any claim-point in the list matches the position.
     */
    private boolean anyClaimPointFor(List<Waypoint> waypoints, Claim claim) {
        for (Waypoint waypoint : waypoints) {
            if (samePos(waypoint, claim) && isClaimPoint(waypoint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if the claim matches the position of the waypoint.
     */
    private boolean samePos(Waypoint waypoint, Claim claim) {
        return claim.pos().x() == waypoint.getX() && claim.pos().z() == waypoint.getZ();
    }

    /**
     * @return {@code true} if any claim in the list matches the position of the waypoint.
     */
    private boolean anyClaimMatches(List<Claim> claims, Waypoint waypoint) {
        for (Claim claim : claims) {
            if (samePos(waypoint, claim)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replaces the existing set of waypoints with the provided set, then saves the world data.
     */
    private void saveWaypoints(
            MinimapSession session,
            MinimapWorld world,
            List<Waypoint> waypoints
    ) {
        try {
            getWaypointSet(world).clear();
            getWaypointSet(world).addAll(waypoints);
            session.getWorldManagerIO().saveWorld(world);
        } catch (IOException e) {
            ClaimPoints.LOG.error("Unable to save waypoints.", e);
        }
    }

    /**
     * Adds new claim-points for all the claims which do not already have one.
     *
     * @return the number of claim-points added.
     */
    public int add(List<Claim> claims) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        int added = 0;
        for (Claim claim : claims) {
            if (!anyClaimPointFor(waypoints, claim)) {
                waypoints.add(new Waypoint(
                        claim.pos().x(),
                        0,
                        claim.pos().z(),
                        String.format(Config.cpSettings().nameFormat, claim.size()),
                        Config.cpSettings().alias,
                        WaypointColor.fromIndex(Config.cpSettings().colorIdx),
                        WaypointPurpose.NORMAL,
                        false,
                        false
                ));
                added++;
            }
        }

        saveWaypoints(session, world, waypoints);
        return added;
    }

    /**
     * Removes all claim-points which do not have a corresponding claim.
     *
     * @return the number of claim-points removed.
     */
    public int clean(List<Claim> claims) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        int startSize = waypoints.size();
        waypoints.removeIf((wp) -> isClaimPoint(wp) && !anyClaimMatches(claims, wp));
        int removed = startSize - waypoints.size();

        saveWaypoints(session, world, waypoints);
        return removed;
    }

    /**
     * Removes all claim-points which do not have a corresponding claim, updates the size of all the
     * others, and adds new claim-points for all the claims which do not already have one.
     *
     * @return the result of the operation.
     */
    public WaypointResult update(List<Claim> claims) {
        int added = add(claims);

        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        int updated = 0;
        int removed = 0;
        Iterator<Waypoint> waypointsIter = waypoints.iterator();
        while (waypointsIter.hasNext()) {
            Waypoint waypoint = waypointsIter.next();
            if (Config.cpSettings().nameCompiled.matcher(waypoint.getName()).find()) {
                switch (ignoreUpdatedRemove(claims, waypoint)) {
                    // Case 0 is no change
                    case 1 -> updated++;
                    case 2 -> {
                        waypointsIter.remove();
                        removed++;
                    }
                }
            }
        }

        saveWaypoints(session, world, waypoints);
        return new WaypointResult(added, updated, removed);
    }

    /**
     * @return 0: ignore waypoint, 1: waypoint was updated, 2: waypoint should be removed.
     */
    private int ignoreUpdatedRemove(List<Claim> claims, Waypoint waypoint) {
        for (Claim claim : claims) {
            if (samePos(waypoint, claim)) {
                Matcher matcher = Config.cpSettings().nameCompiled.matcher(waypoint.getName());
                if (matcher.find()) {
                    // Waypoint is a claim-point; update the size if necessary
                    try {
                        int claimPointSize = Integer.parseInt(matcher.group(1));
                        if (claimPointSize == claim.size()) {
                            return 0;
                        } else {
                            waypoint.setName(String.format(
                                    Config.cpSettings().nameFormat,
                                    claim.size()
                            ));
                            return 1;
                        }
                    } catch (IndexOutOfBoundsException | NumberFormatException e) {
                        ClaimPoints.LOG.error(
                                "Error parsing ClaimPoint {}; Name matches stored pattern but does not have an integer in capturing group #1.",
                                waypoint.getName()
                        );
                        return 0;
                    }
                }
            }
        }
        // No claim matches, so we remove if the waypoint is a claim-point
        return isClaimPoint(waypoint) ? 2 : 0;
    }

    /**
     * Enables all claim-points.
     *
     * @return the number of claim-points enabled.
     */
    public int showAll() {
        return setAllDisabled(false);
    }

    /**
     * Disables all claim-points.
     *
     * @return the number of claim-points disabled.
     */
    public int hideAll() {
        return setAllDisabled(true);
    }

    /**
     * Sets the visibility state of all claim-points.
     *
     * @return the number of claim-points whose state was altered.
     */
    private int setAllDisabled(boolean disabled) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        int altered = 0;
        for (Waypoint waypoint : waypoints) {
            if (isClaimPoint(waypoint)) {
                if (waypoint.isDisabled() != disabled) {
                    waypoint.setDisabled(disabled);
                    altered++;
                }
            }
        }

        saveWaypoints(session, world, waypoints);
        return altered;
    }

    /**
     * Removes all claim-points.
     *
     * @return the number of claim-points removed.
     */
    public int clearAll() {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        int startSize = waypoints.size();
        waypoints.removeIf(this::isClaimPoint);
        int removed = startSize - waypoints.size();

        saveWaypoints(session, world, waypoints);
        return removed;
    }

    /**
     * Changes the name of all claim-points to the specified format.
     */
    public void setNameFormat(String format) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        for (Waypoint waypoint : waypoints) {
            Matcher matcher = Config.cpSettings().nameCompiled.matcher(waypoint.getName());
            if (matcher.find()) {
                try {
                    waypoint.setName(String.format(format, Integer.parseInt(matcher.group(1))));
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    ClaimPoints.LOG.error(
                            "Error parsing ClaimPoint {}; Name matches stored pattern but does not have an integer in capturing group #1.",
                            waypoint.getName()
                    );
                }
            }
        }

        saveWaypoints(session, world, waypoints);
    }

    /**
     * Sets the initials of all claim-points to the specified value.
     */
    public void setInitials(String alias) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(world);

        for (Waypoint waypoint : waypoints) {
            if (isClaimPoint(waypoint)) {
                waypoint.setInitials(alias);
            }
        }

        saveWaypoints(session, world, waypoints);
    }

    /**
     * Sets the color of all claim-points to the specified value.
     */
    public void setColor(int colorIdx) {
        MinimapSession session = getSession();
        MinimapWorld world = getCurrentWorld(session);
        List<Waypoint> waypoints = getWaypoints(getCurrentWorld(getSession()));

        for (Waypoint waypoint : waypoints) {
            if (isClaimPoint(waypoint)) {
                waypoint.setWaypointColor(WaypointColor.fromIndex(colorIdx));
            }
        }

        saveWaypoints(session, world, waypoints);
    }
}
