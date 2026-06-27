package net.bittorn.supervisor.api;

import com.sun.net.httpserver.HttpServer;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.api.handlers.OnlinePlayerHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class APIManager {
    public static void startServer() {
        try {
            var port = ConfigCache.port;
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/api/online", new OnlinePlayerHandler());

            // TODO move to separate thread
            server.setExecutor(null);
            server.start();

            Supervisor.LOGGER.debug("Web server is running on port {}", port);
        } catch (IOException e) {
            Supervisor.LOGGER.error("Error starting web server: {}", e.getMessage());
        }
    }
}
