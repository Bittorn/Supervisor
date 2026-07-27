package net.bittorn.supervisor.seen;

import com.mojang.authlib.GameProfile;
import net.bittorn.supervisor.Supervisor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

public class SeenManager {
    public static void setPlayerSeen(GameProfile gameProfile) {
        Scoreboard scoreboard = Supervisor.SERVER.getScoreboard();
        ScoreHolder scoreHolder = ScoreHolder.fromGameProfile(gameProfile);
        Objective objective = getObjective();

        Supervisor.LOGGER.info("Setting last seen for {}: {}", gameProfile.getName(), getNow());
        scoreboard.getOrCreatePlayerScore(scoreHolder, objective).set(getNow()); // good until 18 Jan 2038.
    }

    public static Objective getObjective() {
        Scoreboard scoreboard = Supervisor.SERVER.getScoreboard();
        Objective objective = Supervisor.SERVER.getScoreboard().getObjective("last_seen");

        if (objective == null) {
            Supervisor.LOGGER.warn("No objective last_seen found, creating");
            objective = scoreboard.addObjective("last_seen", ObjectiveCriteria.DUMMY, Component.literal("Last seen"), ObjectiveCriteria.RenderType.INTEGER, true, null);
        }

        return objective;
    }

    public static boolean hasBeenSeen(GameProfile gameProfile) {
        Scoreboard scoreboard = Supervisor.SERVER.getScoreboard();
        ScoreHolder scoreHolder = ScoreHolder.fromGameProfile(gameProfile);

        Objective objective = getObjective();

        ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);

        int score = scoreAccess.get();
        return score != 0;
    }

    public static int getPlayerSeen(GameProfile gameProfile) {
        Supervisor.LOGGER.info("Getting last seen for {}", gameProfile.getName());

        if (Supervisor.SERVER.getPlayerList().getPlayer(gameProfile.getId()) != null) {
            Supervisor.LOGGER.info("Player is currently online, setting to now");
            setPlayerSeen(gameProfile);
        }

        Scoreboard scoreboard = Supervisor.SERVER.getScoreboard();
        ScoreHolder scoreHolder = ScoreHolder.fromGameProfile(gameProfile);

        Objective objective = getObjective();

        ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(scoreHolder, objective);

        int score = scoreAccess.get();
        if (score == 0) {
            Supervisor.LOGGER.warn("No last seen found for {}", gameProfile.getName());
        }

        return score;
    }

    private static int getNow() {
        return (int) Instant.now().getEpochSecond();
    }

    public static String getFormattedPlayerSeen(GameProfile gameProfile) {
        Date date = new Date(getPlayerSeen(gameProfile)*1000L);
        DateFormat df = new SimpleDateFormat("dd MMM yyyy hh:mm:ss ZZ");
        df.setTimeZone(TimeZone.getTimeZone("Australia/Melbourne"));

        return df.format(date);
    }
}
