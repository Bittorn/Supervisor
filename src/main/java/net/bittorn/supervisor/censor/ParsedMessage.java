package net.bittorn.supervisor.censor;

import java.util.ArrayList;
import java.util.List;

public class ParsedMessage {
    public String message;
    public List<String> matches = new ArrayList<>();

    public CensorManager.Severity maximumSeverity = CensorManager.Severity.LOW;
}
