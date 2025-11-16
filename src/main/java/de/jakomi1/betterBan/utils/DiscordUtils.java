package de.jakomi1.betterBan.utils;

import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static de.jakomi1.betterBan.BetterBan.plugin;

public class DiscordUtils {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Sendet eine einfache Nachricht ohne Styling an Discord (asynchron).
     */
    public static void sendMessage(String content) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord-Webhook: Nachricht ist leer, wird nicht gesendet!");
            return;
        }

        runAsync(() -> {
            try {
                String escaped = content.replace("\"", "\\\"").replace("\n", "\\n");
                String jsonPayload = "{\"content\":\"" + escaped + "\"}";
                sendWebhook(jsonPayload);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Fehler beim Senden des Discord-Webhooks:");
                e.printStackTrace();
            }
        });
    }
    public static void sendMessageWithTime(String content) {
        String timeStamped = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + content;
        sendMessage(timeStamped);
    }
    /**
     * Sendet eine stylische Embed-Nachricht mit Aqua [CRAT] Titel (asynchron).
     */
    public static void sendStyledMessage(String content) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord-Webhook: Nachricht ist leer, wird nicht gesendet!");
            return;
        }

        runAsync(() -> {
            try {
                String escaped = content.replace("\"", "\\\"").replace("\n", "\\n");
                String jsonPayload = "{"
                        + "\"embeds\":[{"
                        + "\"description\":\"[CRAT] >> " + escaped + "\","
                        + "\"color\":65535"
                        + "}]"
                        + "}";
                sendWebhook(jsonPayload);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Fehler beim Senden des Discord-Webhooks:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Sendet eine Embed-Nachricht mit benutzerdefinierter Farbe (asynchron).
     */
    public static void sendColoredMessage(String content, int color) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord-Webhook: Nachricht ist leer, wird nicht gesendet!");
            return;
        }

        runAsync(() -> {
            try {
                String escaped = content.replace("\"", "\\\"").replace("\n", "\\n");
                String jsonPayload = "{"
                        + "\"embeds\":[{"
                        + "\"description\":\"[CRAT] >> " + escaped + "\","
                        + "\"color\":" + color
                        + "}]"
                        + "}";
                sendWebhook(jsonPayload);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Fehler beim Senden des Discord-Webhooks:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Hilfsmethode zum Senden eines JSON-Payloads an Discord.
     * Führt eine Netzwerkoperation aus — deshalb nur von einem asynchronen Thread aufrufen.
     */
    private static void sendWebhook(String jsonPayload) throws Exception {
        if(!ConfigUtils.isWebhookEnabled()) return;
        URL url = new URL(ConfigUtils.getWebhook());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 204) {
            Bukkit.getLogger().severe("Discord-Webhook Error: HTTP " + responseCode);
        }
    }

    public static void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
