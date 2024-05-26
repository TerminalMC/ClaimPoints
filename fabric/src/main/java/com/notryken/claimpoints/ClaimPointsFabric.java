package com.notryken.claimpoints;

import com.notryken.claimpoints.command.Commands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class ClaimPointsFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, buildContext) ->
                new Commands<FabricClientCommandSource>().register(Minecraft.getInstance(), dispatcher, buildContext)));

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(ClaimPoints::onEndTick);

        // Main initialization
        ClaimPoints.init();
    }
}
