package net.bittorn.supervisor.censor;

import java.util.ArrayList;
import java.util.List;

public class ParsedMessage {
    public String message;
    public List<String> matches = new ArrayList<>();

    public enum MatchSeverity {
        NONE,
        MILD,
        SEVERE
    }

    public MatchSeverity maximumSeverity = ParsedMessage.MatchSeverity.NONE;
}
