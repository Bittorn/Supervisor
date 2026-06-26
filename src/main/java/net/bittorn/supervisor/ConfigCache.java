package net.bittorn.supervisor;

import net.bittorn.supervisor.censor.CensorManager;
import net.bittorn.supervisor.platform.Services;

import java.util.List;

public class ConfigCache {
    public static boolean debug;
    public static boolean enableWebhook;
    public static String webhookURL;

    // Censor
    public static boolean enableCensor;
    public static int filterBypassPermissionLevel;
    public static CensorManager.CensorAction lowSeverityAction;
    public static List<? extends String> lowSeverityRules;
    public static CensorManager.CensorAction mediumSeverityAction;
    public static List<? extends String> mediumSeverityRules;
    public static CensorManager.CensorAction highSeverityAction;
    public static List<? extends String> highSeverityRules;

    // API
    public static boolean enableApi;
    public static int port;

    public static void updateCache() {
        debug = Services.PLATFORM.debug();
        enableWebhook = Services.PLATFORM.enableWebhook();
        webhookURL = Services.PLATFORM.webhookURL();

        // Censor
        enableCensor = Services.PLATFORM.enableCensor();
        filterBypassPermissionLevel = Services.PLATFORM.filterBypassPermissionLevel();
        lowSeverityAction = Services.PLATFORM.lowSeverityAction();
        lowSeverityRules = Services.PLATFORM.lowSeverityRules();
        mediumSeverityAction = Services.PLATFORM.mediumSeverityAction();
        mediumSeverityRules = Services.PLATFORM.mediumSeverityRules();
        highSeverityAction = Services.PLATFORM.highSeverityAction();
        highSeverityRules = Services.PLATFORM.highSeverityRules();
    }
}
