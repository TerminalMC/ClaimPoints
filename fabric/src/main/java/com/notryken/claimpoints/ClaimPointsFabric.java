package com.notryken.claimpoints;

import com.mojang.brigadier.CommandDispatcher;
import com.notryken.claimpoints.command.Commands;
import com.notryken.claimpoints.util.FabricWaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.commands.CommandBuildContext;

public class ClaimPointsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClaimPoints.init();
        ClaimPoints.setWaypointManager(new FabricWaypointManager());

        ClientTickEvents.END_CLIENT_TICK.register(ClaimPoints::onEndTick);
        ClientCommandRegistrationCallback.EVENT.register(ClaimPointsFabric::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                         CommandBuildContext context) {
        Commands.register(dispatcher, context);
    }
}