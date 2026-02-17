package de.jakomi1.betterBan.utils;

import de.jakomi1.betterBan.database.Database;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.*;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;

public final class ChatBanUtils {
    // Cache for chat-bans: UUID -> ChatBanData
    private static final Map<UUID, ChatBanData> chatBanCache = new HashMap<>();

    private ChatBanUtils() {}

    private static class ChatBanData {
        final long endTimestamp;
        final String reason;

        ChatBanData(long endTimestamp, String reason) {
            this.endTimestamp = endTimestamp;
            this.reason = reason;
        }
    }

    // ==============================================================

    /**
     * Initialize tables and load cache.
     */
    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS chat_bans (
                    uuid TEXT PRIMARY KEY,
                    end_timestamp INTEGER NOT NULL,
                    reason TEXT
                );
            """);

            // Load cache initially
            loadCache();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadCache() {
        chatBanCache.clear();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, end_timestamp, reason FROM chat_bans")) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long end = rs.getLong("end_timestamp");
                String reason = rs.getString("reason");
                chatBanCache.put(uuid, new ChatBanData(end, reason));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================

    /**
     * Create or update a chat-ban.
     */
    public static void chatBan(UUID uuid, long endTimestamp, String reason) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO chat_bans (uuid, end_timestamp, reason)
                 VALUES (?, ?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET
                     end_timestamp = excluded.end_timestamp,
                     reason = excluded.reason;
             """)) {

            ps.setString(1, uuid.toString());
            ps.setLong(2, endTimestamp);
            ps.setString(3, reason);
            ps.executeUpdate();

            // Update cache
            chatBanCache.put(uuid, new ChatBanData(endTimestamp, reason));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void chatPermanentBan(UUID uuid, String reason) {
        chatBan(uuid, -1, reason);
    }

    public static void chatTempBan(UUID uuid, long durationMillis, String reason) {
        chatBan(uuid, System.currentTimeMillis() + durationMillis, reason);
    }

    public static void chatUnban(UUID uuid) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM chat_bans WHERE uuid = ?")) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();

            // Remove from cache
            chatBanCache.remove(uuid);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isChatBanned(UUID uuid) {
        ChatBanData data = chatBanCache.get(uuid);
        if (data != null) return data.endTimestamp == -1 || System.currentTimeMillis() < data.endTimestamp;
        return false;
    }

    public static String getChatReason(UUID uuid) {
        ChatBanData data = chatBanCache.get(uuid);
        return data != null ? data.reason : null;
    }

    public static Long getChatEnd(UUID uuid) {
        ChatBanData data = chatBanCache.get(uuid);
        return data != null ? data.endTimestamp : null;
    }

    public static boolean isChatPermanent(UUID uuid) {
        ChatBanData data = chatBanCache.get(uuid);
        return data != null && data.endTimestamp == -1;
    }

    public static Map<UUID, Map<String,Object>> getAllChatBans() {
        Map<UUID, Map<String,Object>> bans = new LinkedHashMap<>();
        for (Map.Entry<UUID, ChatBanData> entry : chatBanCache.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("end_timestamp", entry.getValue().endTimestamp);
            data.put("reason", entry.getValue().reason);
            bans.put(entry.getKey(), data);
        }
        return bans;
    }

    public static void clearExpiredChatBans() {
        long now = System.currentTimeMillis();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM chat_bans WHERE end_timestamp != -1 AND end_timestamp < ?")) {

            ps.setLong(1, now);
            ps.executeUpdate();

            // Clean cache
            chatBanCache.entrySet().removeIf(e -> e.getValue().endTimestamp != -1 && e.getValue().endTimestamp < now);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================

    /**
     * Message shown to a player who is chat-banned.
     */
    public static String getChatBanMessage(UUID uuid) {
        Long end = getChatEnd(uuid);
        String reason = getChatReason(uuid);

        if (end == null) return chatPrefix + ChatColor.GREEN + "You are not chat-banned.";

        boolean permanent = end == -1;
        String base = permanent
                ? chatPrefix + ChatColor.RED + "You are permanently chat-banned!"
                : chatPrefix + ChatColor.RED + "You are chat-banned for " + BanUtils.formatDuration(end - System.currentTimeMillis()) + "!";

        if (reason != null && !reason.isBlank()) {
            base += ChatColor.GRAY + "\n>> Reason: " + reason;
        }

        return base;
    }

    // Note: formatDuration is reused from BanUtils to avoid duplication.
}
