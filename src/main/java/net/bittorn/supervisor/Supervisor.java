package net.bittorn.supervisor;

import net.bittorn.supervisor.api.APIManager;
import net.bittorn.supervisor.command.CommandHandler;
import net.bittorn.supervisor.seen.SeenManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.text.NumberFormat;
import java.time.Instant;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Supervisor.MODID)
public class Supervisor {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "supervisor";
    public static final String MODVERSION = "0.0.4";
    public static MinecraftServer SERVER;
    // Directly reference a SLF4J logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML recognizes some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Supervisor(IEventBus eventBus, ModContainer modContainer) {
        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (Supervisor) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        NeoForge.EVENT_BUS.addListener(this::registerCommands);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, SupervisorConfig.SPEC, "supervisor.toml");
        eventBus.register(SupervisorConfig.class);
    }

    public void registerCommands(RegisterCommandsEvent event) {
        CommandHandler.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        SERVER = event.getServer();

        if (ConfigCache.enableWebhook) {
            if (ConfigCache.webhookURL.isBlank()) {
                LOGGER.warn("Discord webhook is enabled, but the URL is empty!");
                LOGGER.warn("Will not send messages to webhook");
            } else {
                LOGGER.info("Discord webhook is enabled");
            }
        } else {
            LOGGER.info("Discord webhook is disabled");
        }

        if (ConfigCache.enableCensor) {
            if (ConfigCache.lowSeverityRules.isEmpty() && ConfigCache.mediumSeverityRules.isEmpty() && ConfigCache.highSeverityRules.isEmpty()) {
                LOGGER.warn("Censor is enabled, but no rules are specified!");
                LOGGER.warn("Will not moderate chat messages");
            } else {
                LOGGER.info("Censor is enabled");
            }
        } else {
            LOGGER.info("Censor is disabled");
        }

        if (ConfigCache.enableApi) {
            LOGGER.info("Web API is enabled");
            APIManager.startServer();
        } else {
            LOGGER.info("Web API is disabled");
        }

        LOGGER.info("Supervisor loaded successfully");
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        SeenManager.setPlayerSeen(event.getEntity().getGameProfile());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        SERVER = null;
        APIManager.stopServer();
    }
}
