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

package dev.terminalmc.claimpoints.mixin;

import dev.terminalmc.claimpoints.ClaimPoints;
import dev.terminalmc.claimpoints.util.CommandUtil;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.claimpoints.config.Config.acSettings;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Inject(
            method = "handleLogin",
            at = @At("RETURN")
    )
    private void afterLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (acSettings().servers.contains(ClaimPoints.lastConnectedIp)) {
            acSettings().commands.forEach((str) -> 
                    CommandUtil.addCommand(str, acSettings().commandDelay * 20));
        }
    }
}
