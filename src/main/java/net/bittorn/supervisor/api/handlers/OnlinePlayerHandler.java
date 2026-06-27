package net.bittorn.supervisor.api.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class OnlinePlayerHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        OutputStream os = exchange.getResponseBody();
        String path = exchange.getRequestURI().getPath();

        Supervisor.LOGGER.debug("API request [{}]: {}", exchange.getRemoteAddress().toString().substring(1), path);

        String prefix = "/api/online/";

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

        String response = String.valueOf(isPlayerOnline(player));
        exchange.sendResponseHeaders(200, response.length());
        os.write(response.getBytes());
        os.close();
    }

    private boolean isPlayerOnline(String query) {
        ServerPlayer player = Supervisor.SERVER.getPlayerList().getPlayerByName(query);
        if (player == null) {
            return false;
        }
        if (player.server.getProfilePermissions(player.getGameProfile()) >= ConfigCache.onlineBypassPermissionLevel
                && ConfigCache.onlineBypassPermissionLevel != 0) {
            if (ConfigCache.debug) Supervisor.LOGGER.debug("Player has bypass permission, returning");
            return false;
        }
        return true;
    }
}
