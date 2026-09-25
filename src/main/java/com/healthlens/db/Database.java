package com.healthlens.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DB HELPER: opens JDBC connections to the local SQLite file and makes sure
 * the schema exists.
 *
 * SQLite is not a server you start separately — the whole database is one
 * file ("healthlens.db"), created automatically in the project's working
 * directory the first time a connection is opened. There is no
 * username/password; the JDBC URL below is the entire "connection string".
 *
 * This mirrors the Database.java pattern from the course's JavaFX + SQLite
 * Student CRUD tutorial, applied to HealthLens's own data instead of a
 * students table.
 */
public class Database {

    private static final String URL = "jdbc:sqlite:healthlens.db";

    /** Opens a new connection. Callers are expected to use try-with-resources. */
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /**
     * Creates every table HealthLens needs, if they don't already exist.
     * Safe to call every time the app starts — CREATE TABLE IF NOT EXISTS
     * leaves an existing table (and its data) untouched.
     */
    public static void initializeDatabase() {
        String createPeopleTable = """
                CREATE TABLE IF NOT EXISTS people (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    name         TEXT NOT NULL,
                    health_tip   TEXT,
                    weekly_score INTEGER NOT NULL DEFAULT 0
                )
                """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPeopleTable);
            System.out.println("[HealthLens] Database ready (healthlens.db).");
        } catch (SQLException e) {
            System.out.println("[HealthLens] Could not initialize database: " + e.getMessage());
        }
    }
}
