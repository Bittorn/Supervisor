package net.bittorn.supervisor.mixins;

import net.bittorn.supervisor.censor.CensorManager;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE - 1000)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleChat", cancellable = true, at = @At("HEAD"))
    private void handleChat(ServerboundChatPacket packet, CallbackInfo ci) {

        CensorManager.processMessage(packet, ci, player);
    }

}
