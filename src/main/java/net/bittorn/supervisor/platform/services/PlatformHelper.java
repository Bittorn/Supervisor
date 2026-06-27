package net.bittorn.supervisor.platform.services;

import net.bittorn.supervisor.SupervisorConfig;
import net.bittorn.supervisor.censor.CensorManager;

import java.util.List;

public class PlatformHelper implements IPlatformHelper {

    @Override
    public boolean debug() {
        return SupervisorConfig.DEBUG.get();
    }

    @Override
    public boolean enableWebhook() {
        return SupervisorConfig.ENABLE_WEBHOOK.get();
    }

    @Override
    public String webhookURL() {
        return SupervisorConfig.WEBHOOK_URL.get();
    }

    // region Censor

    @Override
    public boolean enableCensor() {
        return SupervisorConfig.ENABLE_WEBHOOK.get();
    }

    @Override
    public int filterBypassPermissionLevel() {
        return SupervisorConfig.FILTER_BYPASS_PERMISSION_LEVEL.get();
    }

    @Override
    public CensorManager.CensorAction lowSeverityAction() {
        return SupervisorConfig.LOW_SEVERITY_ACTION.get();
    }

    @Override
    public List<? extends String> lowSeverityRules() {
        return SupervisorConfig.LOW_SEVERITY_RULES.get();
    }

    @Override
    public CensorManager.CensorAction mediumSeverityAction() {
        return SupervisorConfig.MEDIUM_SEVERITY_ACTION.get();
    }

    @Override
    public List<? extends String> mediumSeverityRules() {
        return SupervisorConfig.MEDIUM_SEVERITY_RULES.get();
    }

    @Override
    public CensorManager.CensorAction highSeverityAction() {
        return SupervisorConfig.HIGH_SEVERITY_ACTION.get();
    }

    @Override
    public List<? extends String> highSeverityRules() {
        return SupervisorConfig.HIGH_SEVERITY_RULES.get();
    }

    // endregion

    // region API

    @Override
    public boolean enableApi() {
        return SupervisorConfig.ENABLE_API.get();
    }

    @Override
    public int port() {
        return SupervisorConfig.PORT.get();
    }

    @Override
    public int onlineBypassPermissionLevel() { return SupervisorConfig.ONLINE_BYPASS_PERMISSION_LEVEL.get();}

    // endregion
}
