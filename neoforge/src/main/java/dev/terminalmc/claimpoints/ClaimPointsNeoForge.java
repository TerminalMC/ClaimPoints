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

package dev.terminalmc.claimpoints;

import dev.terminalmc.claimpoints.command.Commands;
import dev.terminalmc.claimpoints.gui.screen.ConfigScreenProvider;
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

@Mod(value = ClaimPoints.MOD_ID, dist = Dist.CLIENT)
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
