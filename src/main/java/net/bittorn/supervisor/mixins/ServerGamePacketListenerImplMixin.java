package net.bittorn.supervisor.mixins;

import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = Integer.MAX_VALUE - 1000)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Inject(method = "handleChat", cancellable = true, at = @At("HEAD"))
    private void handleChat(ServerboundChatPacket packet, CallbackInfo ci) {

        if (ConfigCache.debug) Supervisor.LOGGER.debug("Received chat message: {}", packet.message());

        // Allow mods and ops to bypass chat filter (unless disabled)
        if (player.server.getProfilePermissions(player.getGameProfile()) >= ConfigCache.filterBypassPermissionLevel
            && ConfigCache.filterBypassPermissionLevel != 0) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Player has bypass permission, returning");
            return;
        }

        // Check if Censor is enabled
        if (ConfigCache.enableCensor) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Censor is disabled, returning");
            return;
        }

        ParsedMessage parsedMessage = CensorManager.INSTANCE.parseMessage(packet.message());

        // If no matches were found
        if (parsedMessage.matches.isEmpty()) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("No matches were found");
            return;
        }

        if (supervisor$isSingleplayerOrHost(Objects.requireNonNull(player.getServer()))) return;

        // TODO replace with configurable message
        Component messageToPlayer = Component.literal(String.format(CensorManager.CENSOR_FORMAT, parsedMessage.message, parsedMessage.maximumSeverity));

        CensorManager.CensorAction censorAction = switch (parsedMessage.maximumSeverity) {
            case LOW -> ConfigCache.lowSeverityAction;
            case MEDIUM -> ConfigCache.mediumSeverityAction;
            case HIGH -> ConfigCache.highSeverityAction;
        };

        supervisor$processAction(censorAction, player, messageToPlayer, parsedMessage, ci);
    }

    // TODO move logic to CensorManager class

    @Unique
    private boolean supervisor$isSingleplayerOrHost(MinecraftServer server) {
        if (!server.isPublished()) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Game is singleplayer, return");
            return true;
        }

        if (server.isSingleplayerOwner(player.getGameProfile())) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Player is owner, returning");
            return true;
        }
        return false;
    }

    @Unique
    private void supervisor$processAction(CensorManager.CensorAction censorAction, ServerPlayer player, Component messageToPlayer, ParsedMessage parsedMessage, CallbackInfo ci) {
        MinecraftServer server = Objects.requireNonNull(player.getServer());

        if (censorAction != CensorManager.CensorAction.NONE) {
            supervisor$logMessage(player, parsedMessage);
        }

        if (censorAction != CensorManager.CensorAction.NONE && censorAction != CensorManager.CensorAction.LOG) {
            // If message is flagged, then do not continue with vanilla behaviour
            ci.cancel();
            player.sendSystemMessage(messageToPlayer);
        }

        switch (censorAction) {
            case KICK -> supervisor$kickPlayer(messageToPlayer, player);
            case BAN -> supervisor$banPlayer(messageToPlayer, server, player);
        }
    }

    @Unique
    private void supervisor$logMessage(ServerPlayer player, ParsedMessage parsedMessage) {
        DiscordWebhook.reportPlayerMessage(player, parsedMessage);
    }

    @Unique
    private void supervisor$kickPlayer(Component messageToPlayer, ServerPlayer player) {
        player.connection.disconnect(messageToPlayer);
    }

    @Unique
    private void supervisor$banPlayer(Component messageToPlayer, MinecraftServer server, ServerPlayer player) {
        UserBanList banList = server.getPlayerList().getBans();
        UserBanListEntry banListEntry = new UserBanListEntry(player.getGameProfile(), null, Supervisor.MODID, null, messageToPlayer.getString());
        banList.add(banListEntry);

        // Probably not needed, just ripped directly from vanilla code
        ServerPlayer online = server.getPlayerList().getPlayer(player.getUUID());
        if (online != null) {
            online.connection.disconnect(messageToPlayer);
        }
    }

}
