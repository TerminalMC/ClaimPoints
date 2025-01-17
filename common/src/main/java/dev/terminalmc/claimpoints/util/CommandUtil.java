/*
 * Copyright 2025 TerminalMC
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

package dev.terminalmc.claimpoints.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayList;
import java.util.List;

public class CommandUtil {
    private static final List<DelayedCommand> commands = new ArrayList<>();
    
    public static void addCommand(String command, int delayTicks) {
        commands.add(new DelayedCommand(command, delayTicks));
    }
    
    public static void tick(Minecraft mc) {
        ClientPacketListener connection = mc.getConnection();
        if (connection != null && connection.isAcceptingMessages()) {
            commands.removeIf((cmd) -> cmd.tick(connection));
        } else {
            commands.clear();
        }
    }
    
    static class DelayedCommand {
        String command;
        int remainingTicks;

        DelayedCommand(String command, int remainingTicks) {
            this.command = command;
            this.remainingTicks = remainingTicks;
        }

        boolean tick(ClientPacketListener connection) {
            if (--remainingTicks <= 0) {
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                connection.sendCommand(command);
                return true;
            }
            return false;
        }
    }
}
