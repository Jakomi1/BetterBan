package de.jakomi1.betterBan.utils;

import de.jakomi1.betterBan.database.Database;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.*;

import static de.jakomi1.betterBan.BetterBan.chatPrefix;

public final class BanUtils {

    private static final String PREFIX = chatPrefix;

    // Cache for bans: UUID -> BanData
    private static final Map<UUID, BanData> banCache = new HashMap<>();

    private BanUtils() {}

    private static class BanData {
        final long endTimestamp;
        final String reason;

        BanData(long endTimestamp, String reason) {
            this.endTimestamp = endTimestamp;
            this.reason = reason;
        }
    }

    // ==============================================================

    public static void init() {
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bans (
                    uuid TEXT PRIMARY KEY,
                    end_timestamp INTEGER NOT NULL,
                    reason TEXT
                );
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS has_joined (
                    uuid TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                );
            """);

            // Load cache initially
            loadCache();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadCache() {
        banCache.clear();
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT uuid, end_timestamp, reason FROM bans")) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long end = rs.getLong("end_timestamp");
                String reason = rs.getString("reason");
                banCache.put(uuid, new BanData(end, reason));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================

    public static void ban(UUID uuid, long endTimestamp, String reason) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                 INSERT INTO bans (uuid, end_timestamp, reason)
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
            banCache.put(uuid, new BanData(endTimestamp, reason));

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void permanentBan(UUID uuid, String reason) {
        ban(uuid, -1, reason);
    }

    public static void tempBan(UUID uuid, long durationMillis, String reason) {
        ban(uuid, System.currentTimeMillis() + durationMillis, reason);
    }

    public static void unban(UUID uuid) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM bans WHERE uuid = ?")) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();

            // Remove from cache
            banCache.remove(uuid);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isBanned(UUID uuid) {
        BanData data = banCache.get(uuid);
        if (data != null) return data.endTimestamp == -1 || System.currentTimeMillis() < data.endTimestamp;
        return false;
    }

    public static String getReason(UUID uuid) {
        BanData data = banCache.get(uuid);
        return data != null ? data.reason : null;
    }

    public static Long getEnd(UUID uuid) {
        BanData data = banCache.get(uuid);
        return data != null ? data.endTimestamp : null;
    }

    public static boolean isPermanent(UUID uuid) {
        BanData data = banCache.get(uuid);
        return data != null && data.endTimestamp == -1;
    }

    public static Map<UUID, Map<String,Object>> getAllBans() {
        Map<UUID, Map<String,Object>> bans = new LinkedHashMap<>();
        for (Map.Entry<UUID, BanData> entry : banCache.entrySet()) {
            Map<String, Object> data = new HashMap<>();
            data.put("end_timestamp", entry.getValue().endTimestamp);
            data.put("reason", entry.getValue().reason);
            bans.put(entry.getKey(), data);
        }
        return bans;
    }

    public static void clearExpiredBans() {
        long now = System.currentTimeMillis();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM bans WHERE end_timestamp != -1 AND end_timestamp < ?")) {

            ps.setLong(1, now);
            ps.executeUpdate();

            // Clean cache
            banCache.entrySet().removeIf(e -> e.getValue().endTimestamp != -1 && e.getValue().endTimestamp < now);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==============================================================

    public static String getBanMessage(UUID uuid) {
        Long end = getEnd(uuid);
        String reason = getReason(uuid);

        if (end == null) return PREFIX + ChatColor.GREEN + "You are not banned.";

        boolean permanent = end == -1;
        String base = permanent
                ? PREFIX + ChatColor.RED + "You are permanently banned!"
                : PREFIX + ChatColor.RED + "You are banned for " + formatDuration(end - System.currentTimeMillis()) + "!";

        if (reason != null && !reason.isBlank()) {
            base += ChatColor.GRAY + "\nReason: " + reason;
        }

        return base;
    }

    public static String formatDuration(long millis) {
        if (millis <= 0) return "0 seconds";
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        if (seconds > 0) sb.append(seconds).append(seconds == 1 ? " second " : " seconds ");
        return sb.toString().trim();
    }

    // ==============================================================

    public static void markJoined(UUID uuid, String name) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
             INSERT INTO has_joined (uuid, name)
             VALUES (?, ?)
             ON CONFLICT(uuid) DO UPDATE SET name = excluded.name
         """)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean hasJoinedBefore(UUID uuid) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM has_joined WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Optional<String> getName(UUID uuid) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM has_joined WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(rs.getString("name"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

}
