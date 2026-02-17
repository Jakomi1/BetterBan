package de.jakomi1.betterBan.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static de.jakomi1.betterBan.BetterBan.dataFolder;

public class ConfigUtils {
    private static File configFile;
    private static FileConfiguration config;

    // Cache für schnelle Zugriffe
    private static final Map<String, Object> cache = new HashMap<>();

    // Default-Prefix (legacy color codes mit §)
    private static final String DEFAULT_PREFIX = "§7[§4BB§7]";

    // Regex zum Entfernen von Legacy-Farbcodes (&x oder §x), case-insensitive
    // entfernt einfache Codes wie §a oder &c. Für komplexere Hex-Formate müsste erweitert werden.
    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)(?:§|&)[0-9A-FK-OR]");

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

                // Prefix default
                config.set("prefix", DEFAULT_PREFIX);

                config.save(configFile);

                Bukkit.getLogger().info("Created new config.yml with default keys");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            config = YamlConfiguration.loadConfiguration(configFile);

            // Ensure default keys exist
            if (!config.contains("webhook-url")) config.set("webhook-url", "");
            if (!config.contains("enable-webhook")) config.set("enable-webhook", false);
            if (!config.contains("prefix")) config.set("prefix", DEFAULT_PREFIX);

            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Populate cache
        cache.put("webhook-url", config.getString("webhook-url", ""));
        cache.put("enable-webhook", config.getBoolean("enable-webhook", false));
        cache.put("prefix", config.getString("prefix", DEFAULT_PREFIX));
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

    /** --- Prefix-API --- */

    /** Return the raw prefix string from config (may contain § or & codes) */
    public static String getPrefixRawFromConfig() {
        if (!cache.containsKey("prefix")) loadConfig();
        return (String) cache.getOrDefault("prefix", DEFAULT_PREFIX);
    }

    /**
     * Liefert das Prefix als "styled" String — also mit ChatColor (Farb-/Formatcodes aktiv).
     * Unterstützt sowohl '§' als auch '&' als Eingabe.
     *
     * Beispiel:
     * config: "§f[§cBB§f]" -> returns colored string with actual color control characters.
     */
    public static String getPrefixStyled() {
        String raw = getPrefixRawFromConfig();
        if (raw == null) raw = DEFAULT_PREFIX;

        // Normalisiere: ersetze vorhandene § durch & als "alternate code", dann übersetze
        String asAmp = raw.replace('§', '&');
        return ChatColor.translateAlternateColorCodes('&', asAmp) + " ";
    }

    /**
     * Liefert das Prefix "raw" aber **ohne** Legacy-Farbcodes (&x / §x).
     * Nützlich für Logging/Dateinamen/Webhooks wo keine Farbsteuerzeichen gewünscht sind.
     *
     * Beispiel:
     * config: "§f[§cBB§f]" -> returns "[BB]"
     */
    public static String getPrefixRaw() {
        String raw = getPrefixRawFromConfig();
        if (raw == null) raw = DEFAULT_PREFIX;

        // Entferne bekannte Legacy-Farbcodes (z.B. §a oder &c)
        String cleaned = LEGACY_COLOR_PATTERN.matcher(raw).replaceAll("");

        // Falls noch vereinzelt einzelne § oder & Zeichen vorhanden sind, entferne sie
        cleaned = cleaned.replace("§", "").replace("&", "");

        return cleaned;
    }

    /** Update prefix in memory and save to file */
    public static void setPrefix(String prefix) {
        if (prefix == null) throw new IllegalArgumentException("prefix must not be null");
        if (config == null) loadConfig();
        cache.put("prefix", prefix);
        config.set("prefix", prefix);
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
