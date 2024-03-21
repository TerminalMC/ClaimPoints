package com.notryken.claimpoints.util;

import com.mojang.datafixers.util.Pair;
import com.notryken.claimpoints.ClaimPoints;
import net.minecraft.world.phys.Vec2;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.notryken.claimpoints.ClaimPoints.config;

public class FabricWaypointManager implements WaypointManager {
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
        }
        catch (IOException e) {
            ClaimPoints.LOG.error("ClaimPoints: Unable to save waypoints.", e);
        }
    }

    private boolean anyWpMatches(int x, int z, Pattern namePattern, List<Waypoint> wpList) {
        for (Waypoint wp : wpList) {
            if (wp.getX() == x && wp.getZ() == z && namePattern.matcher(wp.getName()).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean anyClaimMatches(Waypoint wp, List<Pair<Vec2,Integer>> claims) {
        for (Pair<Vec2,Integer> claim : claims) {
            if (claim.getFirst().x == wp.getX() && claim.getFirst().y == wp.getZ()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int addClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int added = 0;
        for (Pair<Vec2,Integer> claim : claims) {
            int x = (int)claim.getFirst().x;
            int z = (int)claim.getFirst().y;
            String name = "Claim (" + claim.getSecond() + ")";
            if (!anyWpMatches(x, z, config().cpSettings.nameCompiled, wpList)) {
                wpList.add(new Waypoint(x, 0, z, name, "CP", 2, 0, false, false));
                added++;
            }
        }

        saveWaypoints(session, wpWorld);
        return added;
    }

    @Override
    public int cleanClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (config().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                if (!anyClaimMatches(wp, claims)) {
                    wpListIter.remove();
                    removed++;
                }
            }
        }

        saveWaypoints(session, wpWorld);
        return removed;
    }

    @Override
    public int[] updateClaimPoints(List<Pair<Vec2,Integer>> claims) {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int updated = 0;
        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (config().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
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
                Matcher cpMatcher = config().cpSettings.nameCompiled.matcher(wp.getName());
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
                            wp.setName(String.format(config().cpSettings.nameFormat, claim.getSecond()));
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
        return config().cpSettings.nameCompiled.matcher(wp.getName()).find() ? 2 : 0;
    }

    @Override
    public int showClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int shown = 0;
        for (Waypoint wp : wpList) {
            if (config().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setDisabled(false);
                shown++;
            }
        }
        saveWaypoints(session, wpWorld);
        return shown;
    }

    @Override
    public int hideClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int hidden = 0;
        for (Waypoint wp : wpList) {
            if (config().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wp.setDisabled(true);
                hidden++;
            }
        }
        saveWaypoints(session, wpWorld);
        return hidden;
    }

    @Override
    public int clearClaimPoints() {
        XaeroMinimapSession session = getSession();
        WaypointWorld wpWorld = getWpWorld(session);
        List<Waypoint> wpList = getWpList(wpWorld);

        int removed = 0;
        Iterator<Waypoint> wpListIter = wpList.iterator();
        while (wpListIter.hasNext()) {
            Waypoint wp = wpListIter.next();
            if (config().cpSettings.nameCompiled.matcher(wp.getName()).find()) {
                wpListIter.remove();
                removed++;
            }
        }

        saveWaypoints(session, wpWorld);
        return removed;
    }
}
