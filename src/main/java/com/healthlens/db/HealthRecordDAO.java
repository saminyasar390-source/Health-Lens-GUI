package com.healthlens.db;

import com.healthlens.model.HealthRecord;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** CRUD access for health_records; person_id is a foreign key to people.id. */
public class HealthRecordDAO {
    public void insert(HealthRecord r) throws SQLException {
        String sql="INSERT INTO health_records(person_id,record_date,sleep_hours,water_glasses,exercise_minutes,stress_level) VALUES(?,?,?,?,?,?)";
        try(Connection c=Database.connect(); PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){
            p.setInt(1,r.getPersonId()); p.setString(2,r.getRecordDate()); p.setDouble(3,r.getSleepHours()); p.setDouble(4,r.getWaterGlasses()); p.setDouble(5,r.getExerciseMinutes()); p.setDouble(6,r.getStressLevel()); p.executeUpdate();
            try(ResultSet k=p.getGeneratedKeys()){if(k.next())r.setId(k.getInt(1));}
        }
    }
    public List<HealthRecord> getForPerson(int personId) throws SQLException {
        List<HealthRecord> out=new ArrayList<>();
        String sql="SELECT id,person_id,record_date,sleep_hours,water_glasses,exercise_minutes,stress_level FROM health_records WHERE person_id=? ORDER BY record_date DESC, id DESC";
        try(Connection c=Database.connect();PreparedStatement p=c.prepareStatement(sql)){
            p.setInt(1, personId);
            try(ResultSet rs=p.executeQuery()){while(rs.next())out.add(new HealthRecord(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6),rs.getDouble(7)));}
        }
        return out;
    }

    public List<HealthRecord> getAll() throws SQLException {
        List<HealthRecord> out=new ArrayList<>();
        String sql="SELECT id,person_id,record_date,sleep_hours,water_glasses,exercise_minutes,stress_level FROM health_records ORDER BY id";
        try(Connection c=Database.connect();PreparedStatement p=c.prepareStatement(sql);ResultSet rs=p.executeQuery()){while(rs.next())out.add(new HealthRecord(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getDouble(4),rs.getDouble(5),rs.getDouble(6),rs.getDouble(7)));}
        return out;
    }
    public void update(HealthRecord r) throws SQLException {
        String sql="UPDATE health_records SET person_id=?,record_date=?,sleep_hours=?,water_glasses=?,exercise_minutes=?,stress_level=? WHERE id=?";
        try(Connection c=Database.connect();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,r.getPersonId());p.setString(2,r.getRecordDate());p.setDouble(3,r.getSleepHours());p.setDouble(4,r.getWaterGlasses());p.setDouble(5,r.getExerciseMinutes());p.setDouble(6,r.getStressLevel());p.setInt(7,r.getId());p.executeUpdate();}
    }
    public void delete(int id) throws SQLException {try(Connection c=Database.connect();PreparedStatement p=c.prepareStatement("DELETE FROM health_records WHERE id=?")){p.setInt(1,id);p.executeUpdate();}}
    public void seedDemoDataIfNeeded() throws SQLException {
        if(!getAll().isEmpty())return;
        PersonDAO people=new PersonDAO();
        List<com.healthlens.model.Person> ps=people.getAllPeople();
        if(ps.size()>=3){
            insert(new HealthRecord(ps.get(0).getId(),"2026-09-26",7.5,7,30,3));
            insert(new HealthRecord(ps.get(1).getId(),"2026-09-26",6.5,5,20,6));
            insert(new HealthRecord(ps.get(2).getId(),"2026-09-26",8,8,45,2));
        }
    }
}
