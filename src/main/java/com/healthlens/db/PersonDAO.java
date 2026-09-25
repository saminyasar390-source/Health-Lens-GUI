package com.healthlens.db;

import com.healthlens.model.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) for the "people" table.
 *
 * Every SQL statement HealthLens runs against the "people" table lives here
 * — the GUI code (GuideController) never writes SQL itself, it just calls
 * methods like insertPerson() or deletePerson(). This keeps the database
 * layer swappable and easy to test on its own, same idea as StudentDAO in
 * the course tutorial.
 */
public class PersonDAO {

    // ----- CREATE -----
    /** Inserts a new row and writes the SQLite-generated id back onto the Person object. */
    public void insertPerson(Person person) throws SQLException {
        String sql = "INSERT INTO people(name, health_tip, weekly_score) VALUES (?, ?, ?)";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, person.getName());
            ps.setString(2, person.getHealthTip());
            ps.setInt(3, person.getWeeklyScore());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    person.setId(keys.getInt(1));
                }
            }
        }
    }

    // ----- READ -----
    public List<Person> getAllPeople() throws SQLException {
        String sql = "SELECT id, name, health_tip, weekly_score FROM people ORDER BY id";
        List<Person> people = new ArrayList<>();

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                people.add(new Person(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("health_tip"),
                        rs.getInt("weekly_score")
                ));
            }
        }
        return people;
    }

    public int countPeople() throws SQLException {
        String sql = "SELECT COUNT(*) FROM people";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ----- UPDATE -----
    public void updatePerson(Person person) throws SQLException {
        String sql = "UPDATE people SET name = ?, health_tip = ?, weekly_score = ? WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, person.getName());
            ps.setString(2, person.getHealthTip());
            ps.setInt(3, person.getWeeklyScore());
            ps.setInt(4, person.getId());
            ps.executeUpdate();
        }
    }

    // ----- DELETE -----
    public void deletePerson(int id) throws SQLException {
        String sql = "DELETE FROM people WHERE id = ?";
        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * One-time migration: the Guide's Person table used to be three
     * hardcoded objects created in Java (Amina, Rafi, Nadia). The first
     * time the app runs against a fresh/empty database, those same three
     * rows are inserted here so the demo looks identical — the only
     * difference is the data now lives in SQLite instead of in code, and
     * can be added to, edited, or deleted from the GUI.
     */
    public void seedDemoDataIfNeeded() throws SQLException {
        if (countPeople() > 0) {
            return;
        }
        insertPerson(new Person("Amina", "Drinks 8 glasses of water daily", 92));
        insertPerson(new Person("Rafi", "Walks 30 minutes every morning", 78));
        insertPerson(new Person("Nadia", "Sleeps 8 hours on a fixed schedule", 88));
    }
}
