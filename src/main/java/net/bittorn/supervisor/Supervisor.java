package net.bittorn.supervisor;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Supervisor.MODID)
public class Supervisor {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "supervisor";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML recognizes some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Supervisor(ModContainer modContainer) {
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Supervisor) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, SupervisorConfig.SPEC, "supervisor.toml");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        if (SupervisorConfig.DEBUG.getAsBoolean()) {
            LOGGER.debug("Debug mode is enabled.");
        }

        if (SupervisorConfig.ENABLE_WEBHOOK.getAsBoolean()) {
            if (SupervisorConfig.WEBHOOK_URL.get().isBlank()) {
                LOGGER.warn("Discord webhook is enabled, but the URL is empty!");
                LOGGER.warn("Will not send messages to webhook");
            } else {
                LOGGER.info("Discord webhook is enabled");
            }
        } else {
            LOGGER.info("Discord webhook is disabled");
        }

        if (SupervisorConfig.ENABLE_CENSOR.getAsBoolean()) {
            if (SupervisorConfig.LOW_SEVERITY_RULES.get().isEmpty() && SupervisorConfig.MEDIUM_SEVERITY_RULES.get().isEmpty() && SupervisorConfig.HIGH_SEVERITY_RULES.get().isEmpty()) {
                LOGGER.warn("Censor is enabled, but no rules are specified!");
                LOGGER.warn("Will not moderate chat messages");
            } else {
                LOGGER.info("Censor is enabled");
            }
        } else {
            LOGGER.info("Censor is disabled");
        }

        LOGGER.info("Supervisor loaded successfully");
    }
}
