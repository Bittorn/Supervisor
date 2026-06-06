package net.bittorn.supervisor.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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

import java.time.LocalDate;
import java.util.Date;

public class SupervisorCommandHandler {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> supervisor = Commands.literal("supervisor").requires(commandSourceStack -> commandSourceStack.hasPermission(2));

        // supervisor help
        supervisor.then(Commands.literal("help").executes(ctx -> {
            sendSuccess(ctx, Component.literal("--- Prefixed Commands ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            // Players
            sendClickable(ctx, "/supervisor kick <player> [reason]",        "Kicks player with optional reason",                ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor ban <player> <length> <reason>","Bans player for given length and reason",          ChatFormatting.DARK_RED);
            sendClickable(ctx, "/supervisor permaban <player> <reason>",    "Permanently bans player for given reason",         ChatFormatting.DARK_RED);

            sendSuccess(ctx, Component.literal("--- General Commands ---").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            // Gamemode
            sendClickable(ctx, "/gmc",                                      "Sets self to creative mode",                       ChatFormatting.AQUA);
            sendClickable(ctx, "/gms",                                      "Sets self to survival mode",                       ChatFormatting.AQUA);
            sendClickable(ctx, "/gma",                                      "Sets self to adventure mode",                      ChatFormatting.AQUA);
            sendClickable(ctx, "/gmsp",                                     "Sets self to spectator mode",                      ChatFormatting.AQUA);
            return 1;
        }));

        supervisor.then(Commands.literal("ban")).executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal("Usage: /supervisor ban <player> <length> <reason>").withStyle(ChatFormatting.RED));
            return 1;
        }).then(Commands.argument("player", EntityArgument.player())).executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal("Usage: /supervisor ban <player> <length> <reason>").withStyle(ChatFormatting.RED));
            return 1;
        }).then(Commands.argument("length", IntegerArgumentType.integer(1))).executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal("Usage: /supervisor ban <player> <length> <reason>").withStyle(ChatFormatting.RED));
            return 1;
        }).then(Commands.argument("reason", StringArgumentType.greedyString())).executes(ctx ->
                banPlayer(ctx, EntityArgument.getPlayer(ctx, "player").getGameProfile(), IntegerArgumentType.getInteger(ctx, "length"), StringArgumentType.getString(ctx, "reason")));

        supervisor.then(Commands.literal("permaban")).executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal("Usage: /supervisor permaban <player> <reason>").withStyle(ChatFormatting.RED));
            return 1;
        }).then(Commands.argument("player", EntityArgument.player())).executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal("Usage: /supervisor permaban <player> <reason>").withStyle(ChatFormatting.RED));
            return 1;
        }).then(Commands.argument("reason", StringArgumentType.greedyString())).executes(ctx ->
                banPlayer(ctx, EntityArgument.getPlayer(ctx, "player").getGameProfile(), 0, StringArgumentType.getString(ctx, "reason")));
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
