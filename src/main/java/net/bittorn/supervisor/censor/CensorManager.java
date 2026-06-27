package net.bittorn.supervisor.censor;

import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.webhook.DiscordWebhook;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CensorManager {
    public static final CensorManager INSTANCE = new CensorManager();

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum CensorAction {
        NONE,
        LOG,
        BLOCK,
        KICK,
        BAN
    }

    public static final String CENSOR_FORMAT = """
    §l§c[MESSAGE FLAGGED BY CENSOR]§r
    
    §bYour original message:§r
    %s
    
    §bMaximum severity:§r
    %s
    """;

    private ParsedMessage parseMessage(String message) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.message = message;
        String highlightedMessage = message;

        // Check low severity rules
        for (String rule : ConfigCache.lowSeverityRules) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.LOW);
        }

        // Check medium severity rules
        for (String rule : ConfigCache.mediumSeverityRules) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.MEDIUM);
        }

        // Check high severity rules
        for (String rule : ConfigCache.highSeverityRules) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.HIGH);
        }

        parsedMessage.message = highlightedMessage;

        return parsedMessage;
    }

    public static void processMessage(ServerboundChatPacket packet, CallbackInfo ci, ServerPlayer player) {
        if (ConfigCache.debug) Supervisor.LOGGER.debug("Received chat message: {}", packet.message());

        // Allow mods and ops to bypass chat filter (unless disabled)
        if (player.server.getProfilePermissions(player.getGameProfile()) >= ConfigCache.filterBypassPermissionLevel
                && ConfigCache.filterBypassPermissionLevel != 0) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Player has bypass permission, returning");
            return;
        }

        // Check if Censor is enabled
        if (!ConfigCache.enableCensor) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Censor is disabled, returning");
            return;
        }

        ParsedMessage parsedMessage = INSTANCE.parseMessage(packet.message());

        // If no matches were found
        if (parsedMessage.matches.isEmpty()) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("No matches were found");
            return;
        }

        if (isSingleplayerOrHost(player)) return;

        // TODO replace with configurable message
        Component messageToPlayer = Component.literal(String.format(CENSOR_FORMAT, parsedMessage.message, parsedMessage.maximumSeverity));

        CensorAction censorAction = switch (parsedMessage.maximumSeverity) {
            case LOW -> ConfigCache.lowSeverityAction;
            case MEDIUM -> ConfigCache.mediumSeverityAction;
            case HIGH -> ConfigCache.highSeverityAction;
        };

        processAction(censorAction, player, messageToPlayer, parsedMessage, ci);
    }

    private static boolean isSingleplayerOrHost(ServerPlayer player) {
        MinecraftServer server = Supervisor.SERVER;
        assert server != null;
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

    private static void processAction(CensorAction censorAction, ServerPlayer player, Component messageToPlayer, ParsedMessage parsedMessage, CallbackInfo ci) {
        MinecraftServer server = Supervisor.SERVER;

        if (censorAction != CensorAction.NONE) {
            logMessage(player, parsedMessage);
        }

        if (censorAction != CensorAction.NONE && censorAction != CensorAction.LOG) {
            // If message is flagged, then do not continue with vanilla behaviour
            ci.cancel();
            player.sendSystemMessage(messageToPlayer);
        }

        switch (censorAction) {
            case KICK -> kickPlayer(messageToPlayer, player);
            case BAN -> banPlayer(messageToPlayer, player);
        }
    }

    private static void logMessage(ServerPlayer player, ParsedMessage parsedMessage) {
        DiscordWebhook.reportPlayerMessage(player, parsedMessage);
    }

    private static void kickPlayer(Component messageToPlayer, ServerPlayer player) {
        player.connection.disconnect(messageToPlayer);
    }

    private static void banPlayer(Component messageToPlayer, ServerPlayer player) {
        MinecraftServer server = Supervisor.SERVER;
        UserBanList banList = server.getPlayerList().getBans();
        UserBanListEntry banListEntry = new UserBanListEntry(player.getGameProfile(), null, Supervisor.MODID, null, messageToPlayer.getString());
        banList.add(banListEntry);

        // Probably not needed, just ripped directly from vanilla code
        ServerPlayer online = server.getPlayerList().getPlayer(player.getUUID());
        if (online != null) {
            online.connection.disconnect(messageToPlayer);
        }
    }

    private String matchRule(String rule, ParsedMessage parsedMessage, Severity severity) {
        try {
            Pattern compiledPattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compiledPattern.matcher(parsedMessage.message);

            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String match = matcher.group(0);
                matcher.appendReplacement(sb, "§n" + Matcher.quoteReplacement(match) + "§r");
                parsedMessage.matches.add(match);
                parsedMessage.maximumSeverity = severity;
            }

            matcher.appendTail(sb);
            parsedMessage.message = sb.toString();
        } catch (Exception e) {
            Supervisor.LOGGER.error("{}{}", "Error with rule: " + rule + " - ", e.getMessage()); // a tad long
        }
        return parsedMessage.message;
    }
}
