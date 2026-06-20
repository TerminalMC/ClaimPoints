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

package dev.terminalmc.claimpoints;

import dev.terminalmc.claimpoints.compat.chat.ChatScanner;
import dev.terminalmc.claimpoints.compat.chat.CommandManager;
import dev.terminalmc.claimpoints.compat.minimap.WaypointManager;
import dev.terminalmc.claimpoints.config.Config;
import dev.terminalmc.claimpoints.util.Logging;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClaimPoints {

    public static final String MOD_ID = "claimpoints";
    public static final String MOD_NAME = "ClaimPoints";
    public static final String COMMAND_ALIAS = "cp";
    public static final Logger LOG = Logging.getLogger(MOD_ID);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("C").withStyle(ChatFormatting.AQUA))
            .append(Component.literal("P").withStyle(ChatFormatting.DARK_AQUA))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);
    public static final List<KeyMapping> KEYBINDS = List.of();

    public static @Nullable String lastConnectedIp = null;
    public static WaypointManager waypointManager = null;
    public static List<String> waypointColorNames = null;

    private ClaimPoints() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Client initialization.
     */
    public static void init() {
        waypointManager = new WaypointManager();
        waypointColorNames = waypointManager.getColorNames();
        Config.getAndSave();
    }

    /**
     * Client after-tick event listener.
     */
    public static void afterClientTick(Minecraft mc) {
        CommandManager.tick(mc);
        ChatScanner.checkStop();
    }

    /**
     * Config save listener.
     */
    public static void onConfigSaved(Config config) {
        // If you are maintaining caches based on config, update them here.
    }
}
