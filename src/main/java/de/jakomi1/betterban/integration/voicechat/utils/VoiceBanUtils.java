package de.jakomi1.betterban.integration.voicechat.utils;

import de.jakomi1.betterban.database.Database;
import de.jakomi1.betterban.util.BanUtils;
import de.jakomi1.betterban.util.TextUtils;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static de.jakomi1.betterban.BetterBan.chatPrefix;

public final class VoiceBanUtils {
    private static final Map<UUID, VoiceBanData> voiceBanCache = new HashMap<>();

    private VoiceBanUtils() {}

    private record VoiceBanData(long endTimestamp, String reason) {}

    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS voice_bans (
                    uuid TEXT PRIMARY KEY,
                    end_timestamp INTEGER NOT NULL,
                    reason TEXT
                );
            """);

            loadCache();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadCache() {
        voiceBanCache.clear();

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, end_timestamp, reason FROM voice_bans")) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long end = rs.getLong("end_timestamp");
                String reason = rs.getString("reason");
                voiceBanCache.put(uuid, new VoiceBanData(end, reason));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void voiceBan(UUID uuid, long endTimestamp, String reason) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO voice_bans (uuid, end_timestamp, reason)
                 VALUES (?, ?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET
                     end_timestamp = excluded.end_timestamp,
                     reason = excluded.reason;
             """)) {

            ps.setString(1, uuid.toString());
            ps.setLong(2, endTimestamp);
            ps.setString(3, reason);
            ps.executeUpdate();

            voiceBanCache.put(uuid, new VoiceBanData(endTimestamp, reason));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void voicePermanentBan(UUID uuid, String reason) {
        voiceBan(uuid, -1, reason);
    }

    public static void voiceTempBan(UUID uuid, long durationMillis, String reason) {
        voiceBan(uuid, System.currentTimeMillis() + durationMillis, reason);
    }

    public static void voiceUnban(UUID uuid) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM voice_bans WHERE uuid = ?")) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();

            voiceBanCache.remove(uuid);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isVoiceBanned(UUID uuid) {
        VoiceBanData data = voiceBanCache.get(uuid);
        if (data != null) return data.endTimestamp == -1 || System.currentTimeMillis() < data.endTimestamp;
        return false;
    }

    public static boolean voiceChatBanned(UUID uuid) {
        return isVoiceBanned(uuid);
    }

    public static String getVoiceReason(UUID uuid) {
        VoiceBanData data = voiceBanCache.get(uuid);
        return data != null ? data.reason : null;
    }

    public static Long getVoiceEnd(UUID uuid) {
        VoiceBanData data = voiceBanCache.get(uuid);
        return data != null ? data.endTimestamp : null;
    }

    public static boolean isVoicePermanent(UUID uuid) {
        VoiceBanData data = voiceBanCache.get(uuid);
        return data != null && data.endTimestamp == -1;
    }

    public static Map<UUID, Map<String, Object>> getAllVoiceBans() {
        Map<UUID, Map<String, Object>> bans = new LinkedHashMap<>();
        for (Map.Entry<UUID, VoiceBanData> entry : voiceBanCache.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("end_timestamp", entry.getValue().endTimestamp);
            data.put("reason", entry.getValue().reason);
            bans.put(entry.getKey(), data);
        }
        return bans;
    }

    public static void clearExpiredVoiceBans() {
        long now = System.currentTimeMillis();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM voice_bans WHERE end_timestamp != -1 AND end_timestamp < ?")) {

            ps.setLong(1, now);
            ps.executeUpdate();

            voiceBanCache.entrySet().removeIf(e -> e.getValue().endTimestamp != -1 && e.getValue().endTimestamp < now);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getVoiceBanMessage(UUID uuid) {
        Long end = getVoiceEnd(uuid);
        String reason = getVoiceReason(uuid);

        if (end == null) return chatPrefix + ChatColor.GRAY + TextUtils.lang("messages.voiceban.not_banned");

        boolean permanent = end == -1;
        String base = permanent
                ? chatPrefix + ChatColor.RED + TextUtils.lang("messages.voiceban.permanent")
                : chatPrefix + ChatColor.RED + TextUtils.lang("messages.voiceban.temp", "duration", BanUtils.formatDuration(end - System.currentTimeMillis()));

        if (reason != null && !reason.isBlank()) {
            base += ChatColor.GRAY + "\n" + TextUtils.lang("messages.voiceban.reason", "reason", reason);
        }

        return base;
    }
}