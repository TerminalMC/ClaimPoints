package dev.terminalmc.claimpoints.xaero;

import com.mojang.datafixers.util.Pair;
import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.config.Config;
import net.minecraft.world.phys.Vec2;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.settings.ModSettings;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaypointManager {
    public List<String> getColorNames() {
        return Arrays.asList(ModSettings.ENCHANT_COLOR_NAMES);
    }

    public XaeroMinimapSession getSession() {
        return XaeroMinimapSession.getCurrentSession();
    }

    public WaypointWorld getWpWorld(XaeroMinimapSession session) {
        return session.getWaypointsManager().getCurrentWorld();
    }

    public List<Waypoint> getWpList(WaypointWorld wpWorld) {
        return wpWorld.getCurrentSet().getList();
    }

    public void saveWaypoints(XaeroMinimapSession session, WaypointWorld wpWorld) {
        try {
            session.getModMain().getSettings().saveWaypoints(wpWorld);
        } catch (IOException e) {
            ClaimPoints.LOG.error("Unable to save waypoints.", e);
        }
    }

    /**
     * @return true if any waypoint in the list matches the coordinates and the
     * pattern.
     */
    private boolean anyWpMatches(int x, int z, Pattern namePattern, List<Waypoint> wpList) {
        for (Waypoint wp : wpList) {
            if (wp.getX() == x && wp.getZ() == z && namePattern.matcher(wp.getName()).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if any claim Vec2 in the list matches the coordinates of the
     * waypoint.
     */
    private boolean anyClaimMatches(Waypoint wp, List<Pair<Vec2,Integer>> claims) {
        for (Pair<Vec2,Integer> claim : claims) {
            if (claim.getFirst().x == wp.getX() && claim.getFirst().y == wp.getZ()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds all the claims as waypoints, excluding those for which there is
     * already a waypoint with matching coordinates.
     * @return the number of waypoints added.
     */
    public int addClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int added = 0;
        for (Pair<Vec2,Integer> claim : claims) {
            int x = (int)claim.getFirst().x;
            int z = (int)claim.getFirst().y;
            if (!anyWpMatches(x, z, Config.get().cpSettings.nameCompiled, wpList)) {
                wpList.add(new Waypoint(x, 0, z, String.format(Config.get().cpSettings.nameFormat, claim.getSecond()),
                        Config.get().cpSettings.alias, Config.get().cpSettings.colorIdx, 0, false, false));
                added++;
            }
        }

        saveWaypoints(session, wpWorld);
        return added;
    }

    /**
     * Removes all waypoints matching the claim-waypoint name pattern, which
     * do not have corresponding claims in the specified list.
     * @return the number of waypoints removed.
     */
    public int cleanClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                if (!anyClaimMatches(wp, claims)) {
                    wpListIter.remove();
                    removed++;
                }
            }
        }

        saveWaypoints(session, wpWorld);
        return removed;
    }

    /**
     * For each waypoint matching the claim-waypoint name pattern, if it has
     * a corresponding claim in the specified list, updates the claim size. If
     * it does not have a corresponding claim, deletes it. Finally, adds new
     * waypoints for all claims without existing waypoints.
     * @return the number of waypoints [added, updated, removed]
     */
    public int[] updateClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int updated = 0;
        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                switch(passUpdateOrRemove(wp, claims)) {
                    // Case 0 is no change
                    case 1 -> updated++;
                    case 2 -> {
                        wpListIter.remove();
                        removed++;
                    }
                }
            }
        }

        int added = addClaimPoints(claims);

        saveWaypoints(session, wpWorld);
        return new int[]{added, updated, removed};
    }

    private int passUpdateOrRemove(Waypoint wp, List<Pair<Vec2,Integer>> claims) {
        for (Pair<Vec2,Integer> claim : claims) {
            if (claim.getFirst().x == wp.getX() && claim.getFirst().y == wp.getZ()) {
                // Matches a claim
                Matcher cpMatcher = Config.get().cpSettings.nameCompiled.matcher(wp.getName());
                if (cpMatcher.find()) {
                    // Is a ClaimPoint
                    try {
                        int cpArea = Integer.parseInt(cpMatcher.group(1));
                        if (cpArea == claim.getSecond()) {
                            // Area matches claim area, pass
                            return 0;
                        }
                        else {
                            // Area does not match claim area, update
                            wp.setName(String.format(Config.get().cpSettings.nameFormat, claim.getSecond()));
                            return 1;
                        }
                    }
                    catch (IndexOutOfBoundsException e) {
                        // No group 1
                        ClaimPoints.LOG.warn("Error parsing ClaimPoint {}; " +
                                "Name matches stored pattern but does not have a Matcher group 1.", wp.getName());
                        return 0;
                    }
                }
            }
        }
        // No claim matches, so we remove if the waypoint is a ClaimPoint
        return Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find() ? 2 : 0;
    }

    /**
     * Enables all waypoints matching the claim-waypoint name pattern.
     * @return the number of waypoints enabled.
     */
    public int showClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int shown = 0;
        for (Waypoint wp : wpList) {
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setDisabled(false);
                shown++;
            }
        }
        saveWaypoints(session, wpWorld);
        return shown;
    }

    /**
     * Disables all waypoints matching the claim-waypoint name pattern.
     * @return the number of waypoints disabled.
     */
    public int hideClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int hidden = 0;
        for (Waypoint wp : wpList) {
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setDisabled(true);
                hidden++;
            }
        }
        saveWaypoints(session, wpWorld);
        return hidden;
    }

    /**
     * Deletes all waypoints matching the claim-waypoint name pattern.
     * @return the number of waypoints deleted.
     */
    public int clearClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wpListIter.remove();
                removed++;
            }
        }

        saveWaypoints(session, wpWorld);
        return removed;
    }

    /**
     * For each waypoint matching the claim-waypoint name pattern, changes the
     * name to the specified format.
     */
    public void setClaimPointNameFormat(String nameFormat) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        for (Waypoint wp : wpList) {
            Matcher matcher = Config.get().cpSettings.nameCompiled.matcher(wp.getName());
            if (matcher.find()) {
                wp.setName(String.format(nameFormat, Integer.parseInt(matcher.group(1))));
            }
        }

        saveWaypoints(session, wpWorld);
    }

    /**
     * For each waypoint matching the claim-waypoint name pattern, changes the
     * alias to the specified value.
     */
    public void setClaimPointAlias(String alias) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        for (Waypoint wp : wpList) {
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setSymbol(alias);
            }
        }

        saveWaypoints(session, wpWorld);
    }

    /**
     * For each waypoint matching the claim-waypoint name pattern, changes the
     * color to the specified value.
     */
    public void setClaimPointColor(int colorIdx) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        for (Waypoint wp : wpList) {
            if (Config.get().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setColor(colorIdx);
            }
        }

        saveWaypoints(session, wpWorld);
    }
}
