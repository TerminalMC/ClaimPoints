package com.notryken.claimpoints.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.phys.Vec2;

import java.util.List;

public interface WaypointManager {
    List<String> getColorNames();

    int addClaimPoints(List<Pair<Vec2,Integer>> claims);

    int cleanClaimPoints(List<Pair<Vec2,Integer>> claims);

    int[] updateClaimPoints(List<Pair<Vec2,Integer>> claims);

    int showClaimPoints();

    int hideClaimPoints();

    int clearClaimPoints();

    void setClaimPointNameFormat(String nameFormat);

    void setClaimPointAlias(String alias);

    void setClaimPointColor(int colorIdx);
}
