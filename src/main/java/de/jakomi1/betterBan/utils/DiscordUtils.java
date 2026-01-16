package de.jakomi1.betterBan.utils;

import de.jakomi1.betterBan.scheduler.Scheduler;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static de.jakomi1.betterBan.BetterBan.plugin;
import static de.jakomi1.betterBan.scheduler.Scheduler.runAsync;

public class DiscordUtils {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Sends a simple message without styling to Discord (asynchronously).
     */
    public static void sendMessage(String content) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord webhook: message is empty, not sending!");
            return;
        }

        runAsync(() -> {
            try {
                String escaped = content.replace("\"", "\\\"").replace("\n", "\\n");
                String jsonPayload = "{\"content\":\"" + escaped + "\"}";
                sendWebhook(jsonPayload);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error sending Discord webhook:");
                e.printStackTrace();
            }
        });
    }

    public static void sendMessageWithTime(String content) {
        String timeStamped = "[" + LocalTime.now().format(TIME_FORMAT) + "] " + content;
        sendMessage(timeStamped);
    }

    /**
     * Sends a styled embed message with Aqua [CRAT] title (asynchronously).
     */
    public static void sendStyledMessage(String content) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord webhook: message is empty, not sending!");
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
                Bukkit.getLogger().severe("Error sending Discord webhook:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Sends an embed message with a custom color (asynchronously).
     */
    public static void sendColoredMessage(String content, int color) {
        if (content == null || content.isEmpty()) {
            Bukkit.getLogger().warning("Discord webhook: message is empty, not sending!");
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
                Bukkit.getLogger().severe("Error sending Discord webhook:");
                e.printStackTrace();
            }
        });
    }

    /**
     * Helper method to send a JSON payload to Discord.
     * Performs a network operation — should only be called from an asynchronous thread.
     */
    private static void sendWebhook(String jsonPayload) throws Exception {
        if (!ConfigUtils.isWebhookEnabled()) return;
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
            Bukkit.getLogger().severe("Discord webhook error: HTTP " + responseCode);
        }
    }

}
