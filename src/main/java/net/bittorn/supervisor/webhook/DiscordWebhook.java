package net.bittorn.supervisor.webhook;

import net.bittorn.supervisor.SupervisorConfig;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {
    public static void reportPlayerMessage(ServerPlayer player, String message, int matchCount, String maximumSeverity) {
        final String MESSAGE_FORMAT = """
                {
                    "embeds": [
                        {
                            "title": "Flagged message",
                            "description": "%s",
                            "color": 4645612,
                            "fields": [
                                {
                                    "name": "Number of flags",
                                    "value": "%,d"
                                },
                                {
                                    "name": "Maximum severity",
                                    "value": "%s"
                                },
                                {
                                    "name": "Player",
                                    "value": "%s",
                                    "inline": true
                                },
                                {
                                    "name": "UUID",
                                    "value": "%s",
                                    "inline": true
                                }
                            ],
                            "thumbnail": {
                                "url": "https://minotar.net/helm/%s"
                            },
                            "footer": {
                                "text": "Supervisor"
                            },
                            "timestamp": "%s"
                        }
                    ]
                }""";

        String username = player.getName().getString();
        String UUID = player.getUUID().toString();

        String content = String.format(MESSAGE_FORMAT, message.replace("§n", "__").replace("§r", "__"), matchCount, maximumSeverity, username, UUID, UUID, OffsetDateTime.now());

        sendWebhook(content);
    }

    private static void sendWebhook(String content) {
        final String webhookURL = SupervisorConfig.WEBHOOK_URL.get();

        if (webhookURL.isBlank()) return;

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SupervisorConfig.WEBHOOK_URL.get()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(content))
                    .build();

            // Send asynchronously
            CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            future.thenAccept(response -> {});
        }
    }
}
