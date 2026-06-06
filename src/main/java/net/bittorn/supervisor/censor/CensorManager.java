package net.bittorn.supervisor.censor;

import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.SupervisorConfig;

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
    
    You have been banned by AutoMod for breaking server chat rules.
    
    §bYour original message:§r
    %s
    
    §bNumber of blocked words:§r
    %,d
    """;



    public ParsedMessage parseMessage(String message) {
        ParsedMessage parsedMessage = new ParsedMessage();
        parsedMessage.message = message;
        String highlightedMessage = message;

        // Check low severity rules
        for (String rule : SupervisorConfig.LOW_SEVERITY_RULES.get()) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.LOW);
        }

        // Check medium severity rules
        for (String rule : SupervisorConfig.MEDIUM_SEVERITY_RULES.get()) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.MEDIUM);
        }

        // Check high severity rules
        for (String rule : SupervisorConfig.HIGH_SEVERITY_RULES.get()) {
            highlightedMessage = matchRule(rule, parsedMessage, Severity.HIGH);
        }

        parsedMessage.message = highlightedMessage;

        return parsedMessage;
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
