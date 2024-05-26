package com.notryken.claimpoints;

import com.notryken.claimpoints.command.Commands;
import com.notryken.claimpoints.gui.screen.ConfigScreenProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(ClaimPoints.MOD_ID)
public class ClaimPointsNeoForge {
    public ClaimPointsNeoForge() {
        // Config screen
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                () -> (mc, parent) -> ConfigScreenProvider.getConfigScreen(parent));

        // Main initialization
        ClaimPoints.init();
    }

    @EventBusSubscriber(modid = ClaimPoints.MOD_ID, value = Dist.CLIENT)
    static class ClientEventHandler {
        // Commands
        @SubscribeEvent
        static void registerClientCommands(RegisterClientCommandsEvent event) {
            new Commands<CommandSourceStack>().register(Minecraft.getInstance(), event.getDispatcher(), event.getBuildContext());
        }

        // Tick events
        @SubscribeEvent
        public static void clientTickEvent(ClientTickEvent.Post event) {
            ClaimPoints.onEndTick(Minecraft.getInstance());
        }
    }
}
