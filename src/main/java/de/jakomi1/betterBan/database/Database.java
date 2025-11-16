package de.jakomi1.betterBan.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static de.jakomi1.betterBan.BetterBan.plugin;

/**
 * Zentrale DB-Utility.
 * - Datenbank-Datei: database.db (im Plugin-Ordner)
 * - Stellt Connection-Factory zur Verfügung
 * - Erstellt grundlegende Tabellen (schema_version + whitelist) beim Init
 * <p>
 * Erweiterungen:
 * - Weitere Basistabellen / Migrations können hier ergänzt werden.
 */
public final class Database {

    private static final String DB_FILE_NAME = "database.db";
    private static final String JDBC_URL;

    static {
        File dbFile = new File(plugin.getDataFolder(), DB_FILE_NAME);
        JDBC_URL = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    private Database() { /* utility */ }

    /**
     * Liefert den JDBC URL (z.B. jdbc:sqlite:/path/to/plugins/…/database.db)
     */
    public static String getJdbcUrl() {
        return JDBC_URL;
    }

    /**
     * Öffnet eine Connection zur zentralen Datenbank.
     * Caller sollte try-with-resources verwenden.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    /**
     * Initialisiert die Datenbank: PRAGMAs und grundlegende Tabellen.
     * Wird beim Plugin-Startup aufgerufen.
     */
    public static void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Aktivieren von Foreign Keys (falls später benötigt)
            stmt.executeUpdate("PRAGMA foreign_keys = ON;");

            // Tabelle für Schema-Versionen / Migrations
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS schema_version (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT UNIQUE NOT NULL,
                        version INTEGER NOT NULL,
                        applied_at TEXT NOT NULL DEFAULT (datetime('now'))
                    );
                    """);
            de.jakomi1.betterBan.utils.BanUtils.init();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
