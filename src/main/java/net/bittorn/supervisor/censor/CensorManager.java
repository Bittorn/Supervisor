package net.bittorn.supervisor.censor;

import net.bittorn.supervisor.SupervisorConfig;

import java.util.ArrayList;
import java.util.List;

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

    public List<String> parseMessage(String message) {
        List<String> matches = new ArrayList<>();
        String maximumSeverity = "";
        int matchCount = 0;

        // Check mild rules
        for (String rule : SupervisorConfig.MILD_RULES.get()) {
            if (rule.matches(message)) {
                matches.add(rule);
                maximumSeverity = "mild";
                matchCount++;
            }
        }

        // Check severe rules
        for (String rule : SupervisorConfig.SEVERE_RULES.get()) {
            if (rule.matches(message)) {
                matches.add(rule);
                maximumSeverity = "severe";
                matchCount++;
            }
        }

        List<String> toReturn = new ArrayList<>(List.of(Integer.toString(matchCount), maximumSeverity));
        toReturn.addAll(matches);
        return toReturn;
    }
}
