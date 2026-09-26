-- HealthLens SQLite relationship demonstration
PRAGMA foreign_keys = ON;

SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name;

PRAGMA table_info(people);
PRAGMA table_info(health_records);
PRAGMA foreign_key_list(health_records);

-- Show the relationship in one query:
SELECT p.id AS person_id, p.name, h.id AS record_id, h.record_date,
       h.sleep_hours, h.water_glasses, h.exercise_minutes, h.stress_level
FROM people p
LEFT JOIN health_records h ON h.person_id = p.id
ORDER BY p.id, h.id;
