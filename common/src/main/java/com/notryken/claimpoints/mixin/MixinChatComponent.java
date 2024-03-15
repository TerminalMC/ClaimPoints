package com.notryken.claimpoints.mixin;

import com.notryken.claimpoints.util.MsgScanner;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = ChatComponent.class)
public class MixinChatComponent {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void checkMessage(Component message,
                              @Nullable MessageSignature $$1,
                              int $$2,
                              @Nullable GuiMessageTag $$3,
                              boolean $$4,
                              CallbackInfo ci) {
        if (MsgScanner.scanning()) {
            if (MsgScanner.scan(message)) ci.cancel();
        }
    }
}
