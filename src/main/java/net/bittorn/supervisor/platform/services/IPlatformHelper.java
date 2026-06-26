package net.bittorn.supervisor.platform.services;

import net.bittorn.supervisor.censor.CensorManager;

import java.util.List;

public interface IPlatformHelper {

    boolean debug();

    boolean enableWebhook();

    String webhookURL();

    // region Censor

    boolean enableCensor();

    int filterBypassPermissionLevel();

    CensorManager.CensorAction lowSeverityAction();

    List<? extends String> lowSeverityRules();

    CensorManager.CensorAction mediumSeverityAction();

    List<? extends String> mediumSeverityRules();

    CensorManager.CensorAction highSeverityAction();

    List<? extends String> highSeverityRules();

    // endregion

    // region API

    boolean enableApi();

    int port();

    // endregion
}
