package net.bittorn.supervisor;

import java.util.List;

import net.bittorn.supervisor.censor.CensorManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SupervisorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment(" Whether to enable debug mode")
            .comment(" Default: false")
            .define("debug", false);

    public static final ModConfigSpec.BooleanValue ENABLE_WEBHOOK = BUILDER
            .comment(" Enable Discord webhook")
            .comment(" Default: true")
            .define("enableWebhook", true);

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL = BUILDER
            .comment(" The Discord webhook URL to send messages to")
            .comment(" Default: \"\"")
            .define("webhookURL", "");

    // region Censor

    public static final ModConfigSpec.BooleanValue ENABLE_CENSOR = BUILDER
            .comment(" Whether to enable Censor (AutoMod)")
            .comment(" Default: true")
            .define("enableCensor", true);

    public static final ModConfigSpec.IntValue FILTER_BYPASS_PERMISSION_LEVEL = BUILDER
            .comment(" Permission level to bypass chat filtering (set to 0 to disable bypassing)")
            .defineInRange("filterBypassPermissionLevel", 4, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.EnumValue<CensorManager.CensorAction> LOW_SEVERITY_ACTION = BUILDER
            .comment(" What action Censor should take on a low-severity violation")
            .defineEnum("lowSeverityAction", CensorManager.CensorAction.LOG);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> LOW_SEVERITY_RULES = BUILDER
            .comment(" A list of regex chat rules to be treated as low severity")
            .defineListAllowEmpty("lowSeverityRules", List.of(), () -> "", SupervisorConfig::validateRule );

    public static final ModConfigSpec.EnumValue<CensorManager.CensorAction> MEDIUM_SEVERITY_ACTION = BUILDER
            .comment(" What action Censor should take on a medium-severity violation")
            .defineEnum("mediumSeverityAction", CensorManager.CensorAction.BLOCK);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MEDIUM_SEVERITY_RULES = BUILDER
            .comment(" A list of regex chat rules to be treated as medium severity")
            .defineListAllowEmpty("mediumSeverityRules", List.of(), () -> "", SupervisorConfig::validateRule );

    public static final ModConfigSpec.EnumValue<CensorManager.CensorAction> HIGH_SEVERITY_ACTION = BUILDER
            .comment(" What action Censor should take on a high-severity violation")
            .defineEnum("highSeverityAction", CensorManager.CensorAction.KICK);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIGH_SEVERITY_RULES = BUILDER
            .comment(" A list of regex chat rules to be treated as high severity")
            .defineListAllowEmpty("highSeverityRules", List.of(), () -> "", SupervisorConfig::validateRule );

    // endregion

    static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading configEvent) {
        Supervisor.LOGGER.debug("Loaded config file {}", configEvent.getConfig().getFileName());
        ConfigCache.updateCache();
    }

    @SubscribeEvent
    public static void onFileChange(final ModConfigEvent.Reloading configEvent) {
        Supervisor.LOGGER.debug("Config just got changed on the file system!");
        ConfigCache.updateCache();
    }

    private static boolean validateRule(final Object obj) {
        return obj instanceof String;
    }
}
