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
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Player has bypass permission, returning");
            return;
        }

        if (!SupervisorConfig.ENABLE_CENSOR.getAsBoolean()) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Censor is disabled, returning");
            return;
        }

        ParsedMessage parsedMessage = CensorManager.INSTANCE.parseMessage(packet.message());

        // If no matches were found
        if (parsedMessage.matches.isEmpty()) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("No matches were found");
            return;
        }

        MinecraftServer server = Objects.requireNonNull(player.getServer());

        // Don't do anything if singleplayer or LAN host
        if (!server.isPublished()) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Game is singleplayer, return");
            return;
        }

        if (server.isSingleplayerOwner(player.getGameProfile())) {
            if (SupervisorConfig.DEBUG.getAsBoolean()) Supervisor.LOGGER.debug("Player is owner, returning");
            return;
        }

        // TODO: replace with configurable message
        Component messageToPlayer = Component.literal(String.format(CensorManager.CENSOR_FORMAT, parsedMessage.message, parsedMessage.matches.size(), parsedMessage.maximumSeverity));

        // TODO: replace with lambda
        switch (parsedMessage.maximumSeverity) {
            case LOW -> processAction(SupervisorConfig.LOW_SEVERITY_ACTION.get(), player, messageToPlayer, parsedMessage, ci);
            case MEDIUM -> processAction(SupervisorConfig.MEDIUM_SEVERITY_ACTION.get(), player, messageToPlayer, parsedMessage, ci);
            case HIGH -> processAction(SupervisorConfig.HIGH_SEVERITY_ACTION.get(), player, messageToPlayer, parsedMessage, ci);
        }
    }

    private void processAction(CensorManager.CensorAction censorAction, ServerPlayer player, Component messageToPlayer, ParsedMessage parsedMessage, CallbackInfo ci) {
        MinecraftServer server = Objects.requireNonNull(player.getServer());

        if (censorAction != CensorManager.CensorAction.NONE) {
            logMessage(player, parsedMessage);
        }

        if (censorAction != CensorManager.CensorAction.NONE && censorAction != CensorManager.CensorAction.LOG) {
            // If message is flagged, then do not continue with vanilla behaviour
            ci.cancel();
            player.sendSystemMessage(messageToPlayer);
        }

        switch (censorAction) {
            case KICK -> kickPlayer(messageToPlayer, player);
            case BAN -> banPlayer(messageToPlayer, server, player);
        }
    }

    private void logMessage(ServerPlayer player, ParsedMessage parsedMessage) {
        DiscordWebhook.reportPlayerMessage(player, parsedMessage);
    }

    private void kickPlayer(Component messageToPlayer, ServerPlayer player) {
        player.connection.disconnect(messageToPlayer);
    }

    private void banPlayer(Component messageToPlayer, MinecraftServer server, ServerPlayer player) {
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
