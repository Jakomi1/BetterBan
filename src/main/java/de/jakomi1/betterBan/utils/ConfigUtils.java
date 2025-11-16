package de.jakomi1.betterBan.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.jakomi1.betterBan.BetterBan.dataFolder;

public class ConfigUtils {
    private static File configFile;
    private static FileConfiguration config;

    // Cache für schnelle Zugriffe
    private static final Map<String, Object> cache = new HashMap<>();

    public static void loadConfig() {
        configFile = new File(dataFolder, "config.yml");

        if (!configFile.exists()) {
            try {
                dataFolder.mkdirs();
                configFile.createNewFile();

                config = YamlConfiguration.loadConfiguration(configFile);

                // Default keys
                config.set("webhook-url", "");
                config.set("enable-webhook", false);

                config.save(configFile);

                System.out.println("Created new config.yml with default keys");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Ensure default keys exist
            if (!config.contains("webhook-url")) config.set("webhook-url", "");
            if (!config.contains("enable-webhook")) config.set("enable-webhook", false);

            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Populate cache
        cache.put("webhook-url", config.getString("webhook-url", ""));
        cache.put("enable-webhook", config.getBoolean("enable-webhook", false));
    }

    /** Get webhook URL from cache */
    public static String getWebhook() {
        if (!cache.containsKey("webhook-url")) loadConfig();
        return (String) cache.getOrDefault("webhook-url", "");
    }

    /** Check if webhook is enabled from cache */
    public static boolean isWebhookEnabled() {
        if (!cache.containsKey("enable-webhook")) loadConfig();
        return (boolean) cache.getOrDefault("enable-webhook", false);
    }

    /** Update webhook URL in memory and save to file */
    public static void setWebhook(String url) {
        if (config == null) loadConfig();
        cache.put("webhook-url", url);
        config.set("webhook-url", url);
        saveConfig();
    }

    /** Update enable-webhook in memory and save to file */
    public static void setEnableWebhook(boolean enabled) {
        if (config == null) loadConfig();
        cache.put("enable-webhook", enabled);
        config.set("enable-webhook", enabled);
        saveConfig();
    }

    /** Save config to file */
    private static void saveConfig() {
        try {
            if (config != null) config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
