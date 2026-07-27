package net.bittorn.supervisor.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.seen.SeenManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.GameType;

import java.time.LocalDate;
import java.util.*;

public class CommandHandler {

    // Only suggest online players
    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> supervisor = Commands.literal("supervisor").requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        // supervisor help
        supervisor.then(Commands.literal("help").executes(ctx -> {
            sendSuccess(ctx, Component.literal("--- Supervisor %s ---".formatted(Supervisor.MODVERSION)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            // Meta
            sendClickable(ctx, "/supervisor help",                              "Lists Supervisor commands",                        ChatFormatting.GREEN);
            sendClickable(ctx, "/supervisor reload",                            "Reloads Supervisor config",                        ChatFormatting.GREEN);

            // Access
//            TODO sendClickable(ctx, "/supervisor warn <player> [reason]",        "Warns player with optional reason",                ChatFormatting.DARK_RED);
            // TODO do not copy <> and [] when clicking
            sendClickable(ctx, "/supervisor kick <player> [reason]",            "Kicks player for optional reason",                 ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor ban <players> <length> <reason>",   "Bans players for given length and reason",         ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor permaban <players> <reason>",       "Permanently bans players for given reason",        ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor unban <players>",                   "(alias for /supervisor pardon)",                   ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor pardon <players>",                  "Pardons players",                                  ChatFormatting.DARK_RED);


            sendSuccess(ctx, Component.literal(" "));

            // Gamemode
            sendClickable(ctx, "/gmc",                                          "Sets self to creative mode",                       ChatFormatting.GOLD);
            sendClickable(ctx, "/gms",                                          "Sets self to survival mode",                       ChatFormatting.GOLD);
            sendClickable(ctx, "/gma",                                          "Sets self to adventure mode",                      ChatFormatting.GOLD);
            sendClickable(ctx, "/gmsp",                                         "Sets self to spectator mode",                      ChatFormatting.GOLD);

            sendClickable(ctx, "/playtime [player]",                            "Gets playtime of player",                          ChatFormatting.AQUA);
            sendClickable(ctx, "/seen [player]",                                "Gets time player was last seen",                   ChatFormatting.AQUA);
            return 1;
        }));

        // region Meta
        supervisor.then(Commands.literal("reload").executes(ctx -> {
            ConfigCache.updateCache();
            sendSuccess(ctx, Component.literal("Supervisor config reloaded!").withStyle(ChatFormatting.DARK_GREEN));
            return 1;
        }));
        // endregion

        // region Access
        supervisor.then(Commands.literal("kick")
                .then(Commands.argument("player", EntityArgument.player()).suggests(ONLINE_PLAYERS).executes(ctx ->
                kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), Component.empty())
        ).then(Commands.argument("reason", MessageArgument.message()).executes(ctx ->
                kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), MessageArgument.getMessage(ctx, "reason"))
        ))));

        supervisor.then(Commands.literal("ban")
                .then(Commands.argument("players", GameProfileArgument.gameProfile()).suggests(ONLINE_PLAYERS)
                        .then(Commands.argument("length", IntegerArgumentType.integer(1))
                                .then(Commands.argument("reason", MessageArgument.message()).executes(ctx ->
                                    banPlayer(ctx, GameProfileArgument.getGameProfiles(ctx, "players"), IntegerArgumentType.getInteger(ctx, "length"), MessageArgument.getMessage(ctx, "reason")))))));

        supervisor.then(Commands.literal("permaban")
                .then(Commands.argument("players", GameProfileArgument.gameProfile()).suggests(ONLINE_PLAYERS)
                        .then(Commands.argument("reason", MessageArgument.message()).executes(ctx ->
                            banPlayer(ctx, GameProfileArgument.getGameProfiles(ctx, "players"), 0, MessageArgument.getMessage(ctx, "reason"))))));

        supervisor.then(Commands.literal("unban")
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .suggests(
                                (ctx, p) -> SharedSuggestionProvider.suggest((ctx.getSource()).getServer().getPlayerList().getBans().getUserList(), p)
                        ).executes(ctx ->
                            unbanPlayer(ctx, GameProfileArgument.getGameProfiles(ctx, "players")))));

        // Quick and dirty clone for /pardon
        supervisor.then(Commands.literal("pardon")
                .then(Commands.argument("players", GameProfileArgument.gameProfile())
                        .suggests(
                                (ctx, p) -> SharedSuggestionProvider.suggest((ctx.getSource()).getServer().getPlayerList().getBans().getUserList(), p)
                        ).executes(ctx ->
                                unbanPlayer(ctx, GameProfileArgument.getGameProfiles(ctx, "players")))));

        dispatcher.register(supervisor);
        // endregion

        // region Gamemode
        LiteralArgumentBuilder<CommandSourceStack> gmc = Commands.literal("gmc").executes(ctx -> setGamemode(ctx, GameType.CREATIVE));
        LiteralArgumentBuilder<CommandSourceStack> gms = Commands.literal("gms").executes(ctx -> setGamemode(ctx, GameType.SURVIVAL));
        LiteralArgumentBuilder<CommandSourceStack> gmsp = Commands.literal("gmsp").executes(ctx -> setGamemode(ctx, GameType.SPECTATOR));
        LiteralArgumentBuilder<CommandSourceStack> gma = Commands.literal("gma").executes(ctx -> setGamemode(ctx, GameType.ADVENTURE));

        // TODO move requirements up to command definitions
        dispatcher.register(gmc.requires(commandSourceStack -> commandSourceStack.hasPermission(4)));
        dispatcher.register(gms.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        dispatcher.register(gmsp.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        dispatcher.register(gma.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        // endregion

        // region Miscellaneous
        LiteralArgumentBuilder<CommandSourceStack> playtime = Commands.literal("playtime")
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        ctx.getSource().sendFailure(Component.literal("Please specify a player: /playtime <player>"));
                        return 0;
                    }
                    return getPlaytime(ctx, Collections.singleton(Objects.requireNonNull(ctx.getSource().getPlayer()).getGameProfile()));
                }
                ).then(Commands.argument("player", GameProfileArgument.gameProfile()).suggests(ONLINE_PLAYERS)
                                .executes(ctx ->
                                        getPlaytime(ctx, GameProfileArgument.getGameProfiles(ctx, "player"))));

        LiteralArgumentBuilder<CommandSourceStack> seen = Commands.literal("seen")
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).suggests(ONLINE_PLAYERS)
                        .executes(ctx ->
                                getPlayerSeen(ctx, GameProfileArgument.getGameProfiles(ctx, "player"))));

        dispatcher.register(playtime);
        dispatcher.register(seen);
        // endregion

    }

    private static int setGamemode(CommandContext<CommandSourceStack> ctx, GameType gameType) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (!ctx.getSource().isPlayer()) {
            ctx.getSource().sendFailure(Component.literal("Cannot change gamemode of a non-player").withStyle(ChatFormatting.RED));
            return 0;
        }
        assert player != null;
        player.setGameMode(gameType);
        sendSuccess(ctx, Component.literal("You are now in ").withStyle(ChatFormatting.GOLD).append(
                Component.literal(gameType.getName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
        ));
        return 1;
    }

    private static int getPlaytime(CommandContext<CommandSourceStack> ctx, final Collection<GameProfile> gameProfiles) {
        GameProfile profile = gameProfiles.iterator().next();

        // TODO get data from offline player
        ServerPlayer player = Supervisor.SERVER.getPlayerList().getPlayer(profile.getId());

        if (player == null || (player.hasPermissions(2) && !ctx.getSource().hasPermission(4))) {
            ctx.getSource().sendFailure(Component.literal("Cannot get playtime of user " + profile.getName()).withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerStatsCounter stats = Supervisor.SERVER.getPlayerList().getPlayerStats(player);

        int ticks = stats.getValue(Stats.CUSTOM, Stats.PLAY_TIME);

        MutableComponent begin;

        if (player == ctx.getSource().getPlayer()) {
            begin = Component.literal("You have played for ").withStyle(ChatFormatting.GOLD);
        } else {
            begin = Component.literal(profile.getName()).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" has played for ").withStyle(ChatFormatting.GOLD));
        }

        sendSuccess(ctx, begin.append(Component.literal(formatPlaytime(ticks)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        return 1;
    }

    private static String formatPlaytime(int ticks) {
        int totalSeconds = ticks / 20;

        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        List<String> components = new ArrayList<>(4);

        if (days > 0) {
            components.add(days + (days == 1 ? " day" : " days"));
        }

        if (hours > 0) {
            components.add(hours + (hours == 1 ? " hour" : " hours"));
        }

        if (minutes > 0) {
            components.add(minutes + (minutes == 1 ? " minute" : " minutes"));
        }

        if (seconds > 0 || components.isEmpty()) {
            components.add(seconds + (seconds == 1 ? " second" : " seconds"));
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < components.size(); i++) {
            result.append(components.get(i));

            if (i < components.size() - 2) {
                result.append(", ");
            } else if (i == components.size() - 2) {
                result.append(" and ");
            }
        }

        result.append(".");

        return result.toString();
    }

    private static int getPlayerSeen(CommandContext<CommandSourceStack> ctx, final Collection<GameProfile> gameProfiles) {
        GameProfile player = gameProfiles.iterator().next();

        // TODO make this work for non-ops
        if ((Supervisor.SERVER.getPlayerList().isOp(player) && !ctx.getSource().hasPermission(4)) || !SeenManager.hasBeenSeen(player)) {
            ctx.getSource().sendFailure(Component.literal("Cannot get last seen of user " + player.getName()).withStyle(ChatFormatting.RED));
            return 0;
        }

        Component component;

        if (Supervisor.SERVER.getPlayerList().getPlayer(player.getId()) != null) {
            component = Component.literal(player.getName()).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" is currently online").withStyle(ChatFormatting.GOLD));
        } else {
            component = Component.literal(player.getName()).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" was last seen on ").withStyle(ChatFormatting.GOLD)
                            .append(Component.literal(SeenManager.getFormattedPlayerSeen(player)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }

        sendSuccess(ctx, component);
        return 1;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player, Component reason) /* throws CommandSyntaxException */ {
        Component cReason = Component.literal("You have been kicked from the server.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component actualReason = Component.literal("""
                You have been kicked from the server.
               
                Reason:\s
               """
        ).append(reason).withStyle(ChatFormatting.RED);

        // TODO check if player is online
        // do we need to? won't return ServerPlayer if player isn't already online

        player.connection.disconnect(reason.getString().isEmpty() ? cReason : actualReason);
        sendSuccess(ctx, Component.literal("%s has been kicked".formatted(player.getName())).withStyle(ChatFormatting.DARK_GREEN));
        return 1;
    }

    private static int banPlayer(CommandContext<CommandSourceStack> ctx, final Collection<GameProfile> gameProfiles, int length, Component reason) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UserBanList userBanList = source.getServer().getPlayerList().getBans();
        int count = 0;

        Date date = java.sql.Date.valueOf(LocalDate.now().plusDays(length));

        Date currentDate = java.sql.Date.valueOf(LocalDate.now());

        for (GameProfile gameProfile : gameProfiles) {
            if (!userBanList.isBanned(gameProfile)) {
                UserBanListEntry userBanListEntry = new UserBanListEntry(gameProfile, currentDate, source.getTextName(), length == 0 ? null : date, reason.getString());
                userBanList.add(userBanListEntry);
                count++;
                sendSuccess(ctx, Component.translatable("commands.ban.success", Component.literal(gameProfile.getName()), userBanListEntry.getReason()));
                ServerPlayer serverplayer = source.getServer().getPlayerList().getPlayer(gameProfile.getId());
                if (serverplayer != null) {
                    serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
                }
            }
        }

        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.translatable("commands.ban.failed")).create();
        } else {
            return count;
        }
    }

    private static int unbanPlayer(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> gameProfiles) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UserBanList userBanList = source.getServer().getPlayerList().getBans();
        int count = 0;

        for (GameProfile gameProfile : gameProfiles) {
            if (userBanList.isBanned(gameProfile)) {
                Supervisor.LOGGER.debug("Unbanning {}", gameProfile.getName());
                userBanList.remove(gameProfile);
                count++;
                sendSuccess(ctx, Component.translatable("commands.pardon.success", Component.literal(gameProfile.getName())));
                Supervisor.LOGGER.debug("Success!");
            }
        }

        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.translatable("commands.pardon.failed")).create();
        } else {
            return count;
        }
    }

    private static void sendSuccess(CommandContext<CommandSourceStack> ctx, Component component) {
        ctx.getSource().sendSuccess(() -> component, true);
    }

    // TODO beef up the styling here
    private static void sendClickable(CommandContext<CommandSourceStack> ctx, String command, String description, ChatFormatting color) {
        sendSuccess(ctx, Component.literal("• ").withStyle(color)
                        .append(Component.literal(command).withStyle(Style.EMPTY
                                .withColor(ChatFormatting.WHITE)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to use")))))
                        .append(Component.literal(" — " + description).withStyle(ChatFormatting.GRAY))
        );
    }

}
