package net.bittorn.supervisor;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

public class SupervisorConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // region AutoMod defaults

    // Matches 'nigger'
    private static final String nSlurRule = "(n|\\|\\\\||ո|ռ|\uD83C\uDD5D|\uD835\uDD79)+\\s*(i|1|!|\\||l|ı|ɩ|ɪ|ӏ|Ꭵ|ꙇ|ꭵ|ǀ|Ι|І|Ӏ|׀|ו|ן|١|۱|ا|Ⲓ|ⵏ|ꓲ|\uD800\uDE8A|\uD800\uDF09|\uD800\uDF20|\uD81B\uDF28|ﺍ|ﺎ|￨|\uD83C\uDD58|\uD835\uDD74)+\\s*(g|9|ƍ|ɡ|ᶃ|\uD83C\uDD56|\uD835\uDD72){2,}\\s*(e|3|£|е|ҽ|ꬲ|\uD83C\uDD54|\uD835\uDD70)+\\s*(r|г|ᴦ|ⲅ|ꭇ|ꭈ|ꮁ|\uD83C\uDD61|\uD835\uDD7D)+";

    // Matches 'faggot'
    private static final String fSlurRule = "(f|ƒ|£|ẝ|ꞙ|ꬵ|\uD83C\uDD55|\uD835\uDDBF|\uD835\uDD71)+\\s*(a|4|@|∆|/-\\\\|/_\\\\|Д|ɑ|а|\uD83C\uDD50|\uD835\uDDBA|\uD835\uDD6C)+\\s*(g|9|ƍ|ɡ|ᶃ|\uD83C\uDD56|\uD835\uDD72){2,}\\s*(o|0|σ|о|ס|ه|٥|ھ|ہ|ە|۵|०|੦|૦|௦|ం|౦|ಂ|೦|ം|ං|๐|໐|ဝ|၀|ჿ|ᴏ|ᴑ|ⲟ|ꬽ|\uD801\uDCEA|\uD806\uDCC8|\uD806\uDCD7|ﮦ|ﻩ|ｏ|○|◍|●|\uD83C\uDD5E|\uD835\uDD7A)+\\s*(t|7|\uD83C\uDD63|\uD835\uDDCD|\uD835\uDD7F)+";

    // Matches 'chink'
    private static final String chSlurRule = "(c|\\(|€|ϲ|с|ᴄ|ⲥ|ꮯ|\uD83C\uDD52|\uD835\uDDA2|\uD835\uDD6E)+\\s*(h|\\|-\\||#|\\}\\{|һ|հ|Ꮒ|\uD83C\uDD57|\uD835\uDD73)+\\s*(i|1|!|\\||l|ı|ɩ|ɪ|ӏ|Ꭵ|ꙇ|ꭵ|ǀ|Ι|І|Ӏ|׀|ו|ן|١|۱|ا|Ⲓ|ⵏ|ꓲ|\uD800\uDE8A|\uD800\uDF09|\uD800\uDF20|\uD81B\uDF28|ﺍ|ﺎ|￨|\uD83C\uDD58|\uD835\uDD74)+\\s*(n|\\|\\\\||ո|ռ|\uD83C\uDD5D|\uD835\uDD79)+\\s*(k|\\|<|\uD83C\uDD5A|\uD835\uDD76)+";

    // Matches 'spick'
    private static final String sSlurRule = "(s|5|\\$|§|ƽ|ꜱ|ꮪ|\uD801\uDC48|\uD806\uDCC1|\uD83C\uDD62|ѕ|\uD835\uDD7E)+\\s*(p|р|ⲣ|ք|\uD83C\uDD5F|\uD835\uDDC9|\uD835\uDD7B)+\\s*(i|1|!|\\||l|ı|ɩ|ɪ|ӏ|Ꭵ|ꙇ|ꭵ|ǀ|Ι|І|Ӏ|׀|ו|ן|١|۱|ا|Ⲓ|ⵏ|ꓲ|\uD800\uDE8A|\uD800\uDF09|\uD800\uDF20|\uD81B\uDF28|ﺍ|ﺎ|￨|\uD83C\uDD58|\uD835\uDD74)+\\s*(c|\\(|€|ϲ|с|ᴄ|ⲥ|ꮯ|\uD83C\uDD52|\uD835\uDDA2|\uD835\uDD6E)+\\s*(k|\\|<|\uD83C\uDD5A|\uD835\uDD76)+";

    // Matches 'coon'
    private static final String cSlurRule = "(c|\\(|€|ϲ|с|ᴄ|ⲥ|ꮯ|\uD83C\uDD52|\uD835\uDDA2|\uD835\uDD6E)+\\s*(o|0|σ|о|ס|ه|٥|ھ|ہ|ە|۵|०|੦|૦|௦|ం|౦|ಂ|೦|ം|ං|๐|໐|ဝ|၀|ჿ|ᴏ|ᴑ|ⲟ|ꬽ|\uD801\uDCEA|\uD806\uDCC8|\uD806\uDCD7|ﮦ|ﻩ|ｏ|○|◍|●|\uD83C\uDD5E|\uD835\uDD7A){2,}\\s*(n|\\|\\\\||ո|ռ|\uD83C\uDD5D|\uD835\uDD79)+";

    // Matches 'kike'
    private static final String kSlurRule = "(k|\\|<|\uD83C\uDD5A|\uD835\uDD76)+\\s*(i|1|!|\\||l|ı|ɩ|ɪ|ӏ|Ꭵ|ꙇ|ꭵ|ǀ|Ι|І|Ӏ|׀|ו|ן|١|۱|ا|Ⲓ|ⵏ|ꓲ|\uD800\uDE8A|\uD800\uDF09|\uD800\uDF20|\uD81B\uDF28|ﺍ|ﺎ|￨|\uD83C\uDD58|\uD835\uDD74)+\\s*(k|\\|<|\uD83C\uDD5A|\uD835\uDD76)+\\s*(e|3|£|е|ҽ|ꬲ|\uD83C\uDD54|\uD835\uDD70)+";

    // Matches 'tranny'
    private static final String tSlurRule = "(t|7|\uD83C\uDD63|\uD835\uDDCD|\uD835\uDD7F)+\\s*(r|г|ᴦ|ⲅ|ꭇ|ꭈ|ꮁ|\uD83C\uDD61|\uD835\uDD7D)+\\s*(a|4|@|∆|/-\\\\|/_\\\\|Д|ɑ|а|\uD83C\uDD50|\uD835\uDDBA|\uD835\uDD6C)+\\s*(n|\\|\\\\||ո|ռ|\uD83C\uDD5D|\uD835\uDD79){2,}\\s*(y|¥|ɣ|ʏ|γ|у|ү|ყ|ᶌ|ỿ|ℽ|ꭚ|\uD806\uDCC4|\uD83C\uDD68|\uD835\uDD84)+";

    // endregion

    private static final List<String> defaultMildRules = List.of(chSlurRule, sSlurRule, cSlurRule);
    private static final List<String> defaultSevereRules = List.of(nSlurRule, kSlurRule, tSlurRule, fSlurRule);

    public static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment(" Whether to enable debug mode")
            .comment(" Default: false")
            .define("debug", false);

    public static final ModConfigSpec.IntValue FILTER_BYPASS_PERMISSION_LEVEL = BUILDER
            .comment(" Permission level to bypass chat filtering (set to 0 to disable bypassing)")
            .defineInRange("filterBypassPermissionLevel", 0, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> WEBHOOK_URL = BUILDER
            .comment("The Discord webhook URL to send messages to")
            .comment(" Default: \"\"")
            .define("webhookURL", "");

    public static final ModConfigSpec.BooleanValue SHOULD_KICK_ON_MILD = BUILDER
            .comment(" Whether to kick a player when their message is flagged as containing mild language")
            .comment(" Default: true")
            .define("shouldKickOnMild", true);

    public static final ModConfigSpec.BooleanValue SHOULD_BAN_ON_SEVERE = BUILDER
            .comment(" Whether to kick a player when their message is flagged as containing severe language")
            .comment(" Default: false")
            .define("shouldBanOnSevere", false);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MILD_RULES = BUILDER
            .comment("A list of regex chat rules to be treated as mild language")
            .defineListAllowEmpty("mildRules", defaultMildRules, () -> "", SupervisorConfig::validateRule );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> SEVERE_RULES = BUILDER
            .comment("A list of regex chat rules to be treated as severe language")
            .defineListAllowEmpty("severeRules", defaultSevereRules, () -> "", SupervisorConfig::validateRule );

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateRule(final Object obj) {
        return obj instanceof String;
    }
}
