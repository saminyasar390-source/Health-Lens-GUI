# HealthLens – Assignment Demonstration Guide

## 1. Live JSON Online API
1. Run HealthLens.
2. Click **🌐 Live Online API Test**.
3. The application performs an HTTP GET to `https://dummyjson.com/products/1` on a background executor.
4. The response is parsed using Jackson `ObjectMapper.readTree()`.
5. The dialog shows the endpoint, parsed `id/title/category/price`, and the raw JSON response.
6. Explain: `HTTP GET -> JSON response -> Jackson JsonNode -> extracted Java values -> JavaFX dialog`.

The normal **🌐 JSON API** button still supports HealthLens JSON, sample JSON, and a user-supplied API URL.

## 2. Abstract class + inheritance
`HealthMetric` is an abstract base class. `SleepMetric`, `WaterMetric`, `ExerciseMetric`, and `StressMetric` extend it and override `calculateScore()`.
`ScoreCalculator` stores them as `HealthMetric[]` and calls the overridden method polymorphically.

Demo code to show:
- `HealthMetric` is `abstract`.
- Each concrete metric `extends HealthMetric`.
- Each subclass uses `@Override`.
- `ScoreCalculator` treats all four objects as `HealthMetric` references.

## 3. SQLite relationships
The database now contains:
- `people(id PK, name, health_tip, weekly_score)`
- `health_records(id PK, person_id FK, record_date, sleep_hours, water_glasses, exercise_minutes, stress_level)`

Relationship:
`people.id 1 ---- many health_records.person_id`

`ON DELETE CASCADE` removes a person's related health records when the person is deleted.

Open `healthlens.db` in DB Browser for SQLite and show the two tables and the foreign key relationship.

## 4. Complete GUI CRUD
Open the Guide and go to the SQLite page.
- **CREATE**: enter name, health tip and score, then click CREATE.
- **READ**: the people table is loaded from SQLite when the page opens/refreshed.
- **UPDATE**: select a row, edit the fields, click UPDATE.
- **DELETE**: select a row, click DELETE. Related health records are deleted by the foreign-key cascade.

## 5. Recommended video order
1. GitHub history and commits.
2. Run application and show JavaFX UI.
3. Resize window and explain responsive property bindings.
4. Guide -> SQLite CRUD page.
5. Open DB Browser and show both tables + relationship.
6. Show `HealthMetric` abstract class and subclasses.
7. Show `ScoreCalculator` polymorphism.
8. Open Live Online API Test and show real HTTP/JSON/Jackson result.
9. Show concurrency package and `ExecutorService` / thread pool.
