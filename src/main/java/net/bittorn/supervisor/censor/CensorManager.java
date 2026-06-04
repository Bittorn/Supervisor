package net.bittorn.supervisor.censor;

import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.SupervisorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CensorManager {
    public static final CensorManager INSTANCE = new CensorManager();

    public static final String CENSOR_FORMAT = """
    §l§c[MESSAGE FLAGGED BY AUTOMOD]§r
    §bYour original message:§r
    %s
    §bNumber of blocked words:§r
    %,d
    §bMaximum severity:§r
    %s
    """;

    public static final String BAN_MESSAGE_FORMAT = """
    §l§c[BANNED BY AUTOMOD]§r
    You have been banned by AutoMod for breaking server rules.
    To appeal, open a ticket on the Discord.
    §bYour original message:§r
    %s
    §bNumber of blocked words:§r
    %,d
    """;



    public ParsedMessage parseMessage(String message) {
        ParsedMessage parsedMessage = new ParsedMessage();

        // Check mild rules
        for (String rule : SupervisorConfig.MILD_RULES.get()) {
            try {
                Pattern compiledPattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
                Matcher matcher = compiledPattern.matcher(message);

                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    String match = matcher.group(0);
                    matcher.appendReplacement(sb, "§n" + Matcher.quoteReplacement(match) + "§r");
                    parsedMessage.matches.add(match);
                    parsedMessage.maximumSeverity = ParsedMessage.MatchSeverity.MILD;
                }

                matcher.appendTail(sb);
                message = sb.toString();
            } catch (Exception e) {
                Supervisor.LOGGER.error("{}{}", "Error with pattern: " + rule + " - ", e.getMessage()); // a tad long
            }
        }

        // Check severe rules
        for (String rule : SupervisorConfig.SEVERE_RULES.get()) {
            try {
                Pattern compiledPattern = Pattern.compile(rule, Pattern.CASE_INSENSITIVE);
                Matcher matcher = compiledPattern.matcher(message);

                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    String match = matcher.group(0);
                    matcher.appendReplacement(sb, "§n" + Matcher.quoteReplacement(match) + "§r");
                    parsedMessage.matches.add(match);
                    parsedMessage.maximumSeverity = ParsedMessage.MatchSeverity.SEVERE;
                }

                matcher.appendTail(sb);
                message = sb.toString();
            } catch (Exception e) {
                Supervisor.LOGGER.error("{}{}", "Error with pattern: " + rule + " - ", e.getMessage()); // a tad long
            }
        }

        parsedMessage.message = message;

        return parsedMessage;
    }
}
