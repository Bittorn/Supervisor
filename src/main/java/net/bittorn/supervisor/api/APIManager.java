package net.bittorn.supervisor.api;

import com.sun.net.httpserver.HttpServer;
import net.bittorn.supervisor.ConfigCache;
import net.bittorn.supervisor.Supervisor;
import net.bittorn.supervisor.api.handlers.PlayerInfoHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class APIManager {

    static int port = ConfigCache.port;
    static HttpServer server;

    public static void startServer() {
        try {
            Supervisor.LOGGER.info("Starting web server");
            server = HttpServer.create(new InetSocketAddress(port), 0);

            server.createContext("/api/player", new PlayerInfoHandler());

            // TODO move to separate thread
            server.setExecutor(null);
            server.start();

            Supervisor.LOGGER.info("Web server is running on port {}", port);
        } catch (IOException e) {
            Supervisor.LOGGER.error("Error starting web server: {}", e.getMessage());
        }
    }

    public static void stopServer() {
        Supervisor.LOGGER.info("Stopping web server");
        server.stop(5);
        Supervisor.LOGGER.info("Web server has stopped");
    }
}
