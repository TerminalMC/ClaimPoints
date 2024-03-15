package com.notryken.claimpoints.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.phys.Vec2;

import java.util.List;

public interface WaypointManager {
    int addClaimPoints(List<Pair<Vec2,Integer>> claims);

    int cleanClaimPoints(List<Pair<Vec2,Integer>> claims);

    int showClaimPoints();

    int hideClaimPoints();

    int clearClaimPoints();
}
