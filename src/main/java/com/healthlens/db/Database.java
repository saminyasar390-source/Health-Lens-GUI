package com.healthlens.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** SQLite connection and schema setup for the HealthLens database. */
public class Database {
    private static final String URL = "jdbc:sqlite:healthlens.db";

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initializeDatabase() {
        String createPeopleTable = """
                CREATE TABLE IF NOT EXISTS people (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    health_tip TEXT,
                    weekly_score INTEGER NOT NULL DEFAULT 0
                )
                """;
        String createHealthRecordsTable = """
                CREATE TABLE IF NOT EXISTS health_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    person_id INTEGER NOT NULL,
                    record_date TEXT NOT NULL,
                    sleep_hours REAL NOT NULL DEFAULT 0,
                    water_glasses REAL NOT NULL DEFAULT 0,
                    exercise_minutes REAL NOT NULL DEFAULT 0,
                    stress_level REAL NOT NULL DEFAULT 0,
                    FOREIGN KEY (person_id) REFERENCES people(id) ON DELETE CASCADE
                )
                """;
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPeopleTable);
            stmt.execute(createHealthRecordsTable);
            System.out.println("[HealthLens] Database ready: people -> health_records relationship enabled.");
        } catch (SQLException e) {
            System.out.println("[HealthLens] Could not initialize database: " + e.getMessage());
        }
    }
}
