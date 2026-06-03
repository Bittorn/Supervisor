package net.bittorn.supervisor;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public class SupervisorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue FILTER_BYPASS_PERMISSION_LEVEL = BUILDER
            .comment("Permission level to bypass chat filtering (set to 0 to disable bypassing)")
            .defineInRange("filterBypassPermissionLevel", 2, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL = BUILDER
            .comment("The Discord webhook URL to send messages to")
            .define("webhookURL", "");

    public static final ModConfigSpec.BooleanValue SHOULD_KICK_ON_MILD = BUILDER
            .comment("Whether to kick a player when their message is flagged as containing mild language")
            .define("shouldKickOnMild", true);

    public static final ModConfigSpec.BooleanValue SHOULD_BAN_ON_SEVERE = BUILDER
            .comment("Whether to kick a player when their message is flagged as containing severe language")
            .define("shouldBanOnSevere", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MILD_RULES = BUILDER
            .comment("A list of regex chat rules to be treated as mild language")
            .defineListAllowEmpty("mildRules", List.of(), () -> "", SupervisorConfig::validateRule );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SEVERE_RULES = BUILDER
            .comment("A list of regex chat rules to be treated as severe language")
            .defineListAllowEmpty("severeRules", List.of(), () -> "", SupervisorConfig::validateRule );

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateRule(final Object obj) {
        return obj instanceof String;
    }
}
