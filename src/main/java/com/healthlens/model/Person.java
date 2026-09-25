package com.healthlens.model;

/**
 * Plain model class used to demonstrate TableView + ObservableList in the
 * Guide. Deliberately simple — standard getters/setters so JavaFX's
 * PropertyValueFactory can read them via reflection.
 *
 * Backed by the "people" table in SQLite (see com.healthlens.db). The id
 * field is the SQLite PRIMARY KEY: 0/unset for a Person not yet saved to
 * the database, and the real row id once PersonDAO.insertPerson() has run.
 */
public class Person {

    private int id;
    private String name;
    private String healthTip;
    private int weeklyScore;

    /** Use for a brand-new Person that hasn't been inserted into the database yet. */
    public Person(String name, String healthTip, int weeklyScore) {
        this(0, name, healthTip, weeklyScore);
    }

    /** Use when reconstructing a Person that already exists in the database (id is known). */
    public Person(int id, String name, String healthTip, int weeklyScore) {
        this.id = id;
        this.name = name;
        this.healthTip = healthTip;
        this.weeklyScore = weeklyScore;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHealthTip() { return healthTip; }
    public void setHealthTip(String healthTip) { this.healthTip = healthTip; }

    public int getWeeklyScore() { return weeklyScore; }
    public void setWeeklyScore(int weeklyScore) { this.weeklyScore = weeklyScore; }
}
