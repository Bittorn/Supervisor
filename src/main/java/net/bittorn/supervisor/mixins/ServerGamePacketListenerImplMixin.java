package net.bittorn.supervisor.mixins;

import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.SupervisorConfig;
import net.bittorn.supervisor.censor.CensorManager;
import net.bittorn.supervisor.censor.ParsedMessage;
import net.bittorn.supervisor.webhook.DiscordWebhook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE - 1000)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleChat", cancellable = true, at = @At("HEAD"))
    private void handleChat(ServerboundChatPacket packet, CallbackInfo ci) {

        if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Received chat message: {}", packet.message());

        // Allow mods and ops to bypass chat filter (unless disabled)
        if (player.server.getProfilePermissions(player.getGameProfile()) >= SupervisorConfig.FILTER_BYPASS_PERMISSION_LEVEL.getAsInt()
            && SupervisorConfig.FILTER_BYPASS_PERMISSION_LEVEL.getAsInt() != 0) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Player has bypass permission, returning.");
            return;
        }

        ParsedMessage parsedMessage = CensorManager.INSTANCE.parseMessage(packet.message());

        // If no matches were found
        if (parsedMessage.matches.isEmpty()) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("No matches were found.");
            return;
        }

        MinecraftServer server = Objects.requireNonNull(player.getServer());

        // Don't do anything if singleplayer or LAN host
        if (!server.isPublished()) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Game is singleplayer, not doing anything.");
            return;
        }

        if (server.isSingleplayerOwner(player.getGameProfile())) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Player is owner, not doing anything.");
            return;
        }

        // If message is flagged, then do not continue with vanilla behaviour
        ci.cancel();

        DiscordWebhook.reportPlayerMessage(player, parsedMessage);

        Component messageToPlayer = Component.literal(String.format(CensorManager.CENSOR_FORMAT, parsedMessage.message, parsedMessage.matches.size(), parsedMessage.maximumSeverity));

        if (SupervisorConfig.SHOULD_BAN_ON_SEVERE.getAsBoolean() && parsedMessage.maximumSeverity == ParsedMessage.MatchSeverity.SEVERE) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Banning player for message: {}", parsedMessage.message);
            messageToPlayer = Component.literal(String.format(CensorManager.BAN_MESSAGE_FORMAT, parsedMessage.message, parsedMessage.matches.size()));

            UserBanList banList = server.getPlayerList().getBans();
            UserBanListEntry banListEntry = new UserBanListEntry(player.getGameProfile(), null, Supervisor.MODID, null, messageToPlayer.getString());
            banList.add(banListEntry);

            // Probably not needed, just ripped directly from vanilla code
            ServerPlayer online = server.getPlayerList().getPlayer(player.getUUID());
            if (online != null) {
                online.connection.disconnect(messageToPlayer);
            }

        } else if (SupervisorConfig.SHOULD_KICK_ON_MILD.getAsBoolean() && (parsedMessage.maximumSeverity == ParsedMessage.MatchSeverity.MILD || parsedMessage.maximumSeverity == ParsedMessage.MatchSeverity.SEVERE)) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Kicking player for message: {}", parsedMessage.message);

            player.connection.disconnect(messageToPlayer);
        }

        player.sendSystemMessage(messageToPlayer);
    }

}
