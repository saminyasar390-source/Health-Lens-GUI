package com.healthlens.model;

/** A daily health record linked to one Person through people.id. */
public class HealthRecord {
    private int id;
    private int personId;
    private String recordDate;
    private double sleepHours;
    private double waterGlasses;
    private double exerciseMinutes;
    private double stressLevel;

    public HealthRecord(int id, int personId, String recordDate, double sleepHours, double waterGlasses, double exerciseMinutes, double stressLevel) {
        this.id = id; this.personId = personId; this.recordDate = recordDate;
        this.sleepHours = sleepHours; this.waterGlasses = waterGlasses;
        this.exerciseMinutes = exerciseMinutes; this.stressLevel = stressLevel;
    }
    public HealthRecord(int personId, String recordDate, double sleepHours, double waterGlasses, double exerciseMinutes, double stressLevel) {
        this(0, personId, recordDate, sleepHours, waterGlasses, exerciseMinutes, stressLevel);
    }
    public int getId(){return id;} public void setId(int id){this.id=id;}
    public int getPersonId(){return personId;} public void setPersonId(int personId){this.personId=personId;}
    public String getRecordDate(){return recordDate;} public void setRecordDate(String recordDate){this.recordDate=recordDate;}
    public double getSleepHours(){return sleepHours;} public double getWaterGlasses(){return waterGlasses;}
    public double getExerciseMinutes(){return exerciseMinutes;} public double getStressLevel(){return stressLevel;}
}
