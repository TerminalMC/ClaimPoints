package com.notryken.claimpoints;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
@Mod(ClaimPoints.MOD_ID)
public class ClaimPointsNeoForge {
    public ClaimPointsNeoForge() {
        ClaimPoints.init();
    }
}