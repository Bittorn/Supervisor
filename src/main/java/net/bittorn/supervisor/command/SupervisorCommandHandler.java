package net.bittorn.supervisor.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.GameType;

import java.time.LocalDate;
import java.util.Date;

public class SupervisorCommandHandler {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> supervisor = Commands.literal("supervisor").requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        // supervisor help
        supervisor.then(Commands.literal("help").executes(ctx -> {
            sendSuccess(ctx, Component.literal("--- Supervisor %s ---".formatted(Supervisor.MODVERSION)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
//            sendSuccess(ctx, Component.literal("--- Prefixed Commands ---").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            // Meta
            sendClickable(ctx, "/supervisor reload",                        "Reloads Supervisor config",                        ChatFormatting.GREEN);
            // Players
//            sendClickable(ctx, "/supervisor warn <player> [reason]",        "Warns player with optional reason",                ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor kick <player> [reason]",        "Kicks player with optional reason",                ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor ban <player> <length> <reason>","Bans player for given length and reason",          ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor permaban <player> <reason>",    "Permanently bans player for given reason",         ChatFormatting.DARK_RED);

            sendSuccess(ctx, Component.literal(" "));
            // Gamemode
            sendClickable(ctx, "/gmc",                                      "Sets self to creative mode",                       ChatFormatting.GOLD);
            sendClickable(ctx, "/gms",                                      "Sets self to survival mode",                       ChatFormatting.GOLD);
            sendClickable(ctx, "/gma",                                      "Sets self to adventure mode",                      ChatFormatting.GOLD);
            sendClickable(ctx, "/gmsp",                                     "Sets self to spectator mode",                      ChatFormatting.GOLD);
            return 1;
        }));

        // region Meta commands

        supervisor.then(Commands.literal("reload").executes(ctx -> {
            ConfigCache.updateCache();
            sendSuccess(ctx, Component.literal("Supervisor config reloaded!").withStyle(ChatFormatting.DARK_GREEN));
            return 1;
        }));

        // endregion

        // region Player commands

        supervisor.then(Commands.literal("kick")
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx ->
                kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), "")
        ).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx ->
                kickPlayer(ctx, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "reason"))
        ))));

        supervisor.then(Commands.literal("ban")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("length", IntegerArgumentType.integer(1))
                                .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx ->
                                    banPlayer(ctx, EntityArgument.getPlayer(ctx, "player").getGameProfile(), IntegerArgumentType.getInteger(ctx, "length"), StringArgumentType.getString(ctx, "reason")))))));

        supervisor.then(Commands.literal("permaban")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString()).executes(ctx ->
                            banPlayer(ctx, EntityArgument.getPlayer(ctx, "player").getGameProfile(), 0, StringArgumentType.getString(ctx, "reason"))))));

        // endregion

        // region Gamemode commands

        LiteralArgumentBuilder<CommandSourceStack> gmc = Commands.literal("gmc").executes(ctx -> setGamemode(ctx, GameType.CREATIVE));
        LiteralArgumentBuilder<CommandSourceStack> gms = Commands.literal("gms").executes(ctx -> setGamemode(ctx, GameType.SURVIVAL));
        LiteralArgumentBuilder<CommandSourceStack> gmsp = Commands.literal("gmsp").executes(ctx -> setGamemode(ctx, GameType.SPECTATOR));
        LiteralArgumentBuilder<CommandSourceStack> gma = Commands.literal("gma").executes(ctx -> setGamemode(ctx, GameType.ADVENTURE));

        // endregion

        dispatcher.register(supervisor);

        dispatcher.register(gmc.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        dispatcher.register(gms.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        dispatcher.register(gmsp.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
        dispatcher.register(gma.requires(commandSourceStack -> commandSourceStack.hasPermission(2)));
    }

    private static int setGamemode(CommandContext<CommandSourceStack> ctx, GameType gameType) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (!ctx.getSource().isPlayer()) {
            ctx.getSource().sendFailure(Component.literal("Cannot change gamemode of a non-player").withStyle(ChatFormatting.RED));
            return 0;
        }
        assert player != null;
        player.setGameMode(gameType);
        sendSuccess(ctx, Component.literal("You are now in %s".formatted(gameType.getName())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return 1;
    }

    private static int kickPlayer(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String reason) /* throws CommandSyntaxException */ {
        Component cReason = Component.literal("You have been kicked from the server.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component actualReason = Component.literal("""
                You have been kicked from the server.
               
                Reason:\s
               """
        ).append(reason).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);

        // TODO check if player is online

        player.connection.disconnect(reason.isEmpty() ? cReason : actualReason);
        sendSuccess(ctx, Component.literal("%s has been kicked".formatted(player.getName())).withStyle(ChatFormatting.DARK_GREEN));
        return 1;
    }

    private static int banPlayer(CommandContext<CommandSourceStack> ctx, GameProfile gameProfile, int length, String reason) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        UserBanList userbanlist = source.getServer().getPlayerList().getBans();

        Date date = java.sql.Date.valueOf(LocalDate.now().plusDays(length));

        Date currentDate = java.sql.Date.valueOf(LocalDate.now());

        if (userbanlist.isBanned(gameProfile)) {
            throw new SimpleCommandExceptionType(Component.translatable("commands.ban.failed")).create();
        } else {
            UserBanListEntry userbanlistentry = new UserBanListEntry(gameProfile, currentDate, source.getTextName(), length == 0 ? null : date, reason);
            userbanlist.add(userbanlistentry);
            source.sendSuccess(() -> Component.translatable("commands.ban.success", Component.literal(gameProfile.getName()), userbanlistentry.getReason()), true);
            ServerPlayer serverplayer = source.getServer().getPlayerList().getPlayer(gameProfile.getId());
            if (serverplayer != null) {
                serverplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.banned"));
            }
            return 1;
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
