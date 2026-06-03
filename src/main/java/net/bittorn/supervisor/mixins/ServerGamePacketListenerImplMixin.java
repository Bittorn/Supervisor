package net.bittorn.supervisor.mixins;

import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.SupervisorConfig;
import net.bittorn.supervisor.censor.CensorManager;
import net.bittorn.supervisor.webhook.DiscordWebhook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Date;
import java.util.List;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE - 1000)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleChat", cancellable = true, at = @At("HEAD"))
    private void handleChat(ServerboundChatPacket packet, CallbackInfo ci) {

        // Allow mods and ops to bypass chat filter (unless disabled)
        if (player.server.getProfilePermissions(player.getGameProfile()) >= SupervisorConfig.FILTER_BYPASS_PERMISSION_LEVEL.getAsInt()
            && SupervisorConfig.FILTER_BYPASS_PERMISSION_LEVEL.getAsInt() != 0) return;

        List<String> parsedMessage = CensorManager.INSTANCE.parseMessage(packet.message());

        int matchCount = Integer.getInteger(parsedMessage.getFirst());
        String maximumSeverity = parsedMessage.get(1);

        parsedMessage.remove(1);
        parsedMessage.removeFirst();

        String[] matches = parsedMessage.toArray(new String[0]);

        // If no matches were found
        if (matchCount == 0) return;

        // If message is flagged, then do not continue with vanilla behaviour
        ci.cancel();

        DiscordWebhook.reportPlayerMessage(player, packet.message(), matchCount, maximumSeverity);

        Component messageToPlayer = Component.literal(String.format(CensorManager.CENSOR_FORMAT, packet.message(), matchCount, maximumSeverity));;

        if (SupervisorConfig.SHOULD_BAN_ON_SEVERE.getAsBoolean()) {
            messageToPlayer = Component.literal(String.format(CensorManager.BAN_MESSAGE_FORMAT, packet.message(), matchCount));
            UserBanList banList = player.server.getPlayerList().getBans();
            UserBanListEntry banListEntry = new UserBanListEntry(player.getGameProfile(), null, Supervisor.MODID, null, messageToPlayer.getString());

        } else if (SupervisorConfig.SHOULD_KICK_ON_MILD.getAsBoolean()) {
            player.connection.disconnect(messageToPlayer);
            return;
        }

        player.sendSystemMessage(messageToPlayer);
    }

}
