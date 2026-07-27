package net.bittorn.supervisor.api.handlers;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.api.responses.PlayerInfoResponse;
import net.bittorn.supervisor.seen.SeenManager;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public class PlayerInfoHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = exchange.getResponseBody();
        String path = exchange.getRequestURI().getPath();

        Supervisor.LOGGER.debug("API request [{}]: {}", exchange.getRemoteAddress().toString().substring(1), path);

        String prefix = "/api/player/";

        if (!path.startsWith(prefix)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        // in case the name has strange characters (which it won't, but IDGAF)
        String player = URLDecoder.decode(
                path.substring(prefix.length()),
                StandardCharsets.UTF_8
        );

        if (player.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        PlayerInfoResponse pir = new PlayerInfoResponse();
        // will never be null
        Optional<GameProfile> profileOptional = Objects.requireNonNull(Supervisor.SERVER.getProfileCache()).get(player);
        if (profileOptional.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        GameProfile profile = profileOptional.get();

        pir.invisible = (Supervisor.SERVER.getProfilePermissions(profile) >= ConfigCache.onlineBypassPermissionLevel
                && ConfigCache.onlineBypassPermissionLevel != 0);

        pir.name = profile.getName();
        pir.seen = getPlayerSeen(profile);
        pir.online = isPlayerOnline(profile);
        pir.available = isPlayerOnline(profile);

        Gson gson = new Gson();
        String response = gson.toJson(pir);

        exchange.sendResponseHeaders(200, response.length());
        os.write(response.getBytes());
        os.close();
    }

    private @Nullable String getPlayerSeen(GameProfile profile) {
        if (Supervisor.SERVER.getProfilePermissions(profile) >= ConfigCache.onlineBypassPermissionLevel
                && ConfigCache.onlineBypassPermissionLevel != 0) {
            return null;
        }

        return SeenManager.getFormattedPlayerSeen(profile);
    }

    private boolean isPlayerOnline(GameProfile profile) {
        return Supervisor.SERVER.getProfilePermissions(profile) < ConfigCache.onlineBypassPermissionLevel
                || ConfigCache.onlineBypassPermissionLevel == 0;
    }
}
