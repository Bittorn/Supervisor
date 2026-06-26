package net.bittorn.supervisor.api.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.bittorn.supervisor.Supervisor;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PlayerHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        OutputStream os = exchange.getResponseBody();
        String path = exchange.getRequestURI().getPath();

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

        if (player == null) {
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
        return player != null;
    }
}
