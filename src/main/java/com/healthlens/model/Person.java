package com.healthlens.model;

/**
 * Plain model class used to demonstrate TableView + ObservableList in the
 * Guide. Deliberately simple — standard getters/setters so JavaFX's
 * PropertyValueFactory can read them via reflection.
 */
public class Person {

    private String name;
    private String healthTip;
    private int weeklyScore;

    public Person(String name, String healthTip, int weeklyScore) {
        this.name = name;
        this.healthTip = healthTip;
        this.weeklyScore = weeklyScore;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHealthTip() { return healthTip; }
    public void setHealthTip(String healthTip) { this.healthTip = healthTip; }

    public int getWeeklyScore() { return weeklyScore; }
    public void setWeeklyScore(int weeklyScore) { this.weeklyScore = weeklyScore; }
}
