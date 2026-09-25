# HealthLens V2 + SQLite Branch Setup

This project is prepared on the branch:

`feature/healthlens-v2-sqlite`

The branch is based on `feature/personalizedHealthRecommendationsSmartEnhancedV2` and integrates the SQLite people database without replacing the V2 dashboard/JSON work.

## What was changed

- Added the SQLite JDBC dependency to `pom.xml`.
- Added `com.healthlens.db.Database` for SQLite connection/schema creation.
- Added `com.healthlens.db.PersonDAO` for CRUD operations and demo-data seeding.
- Updated `Person` with the SQLite `id` field and database constructor.
- Updated `GuideController` so the Page 6 table loads its people from SQLite.
- Updated `Main` to initialize the database at application startup.
- Added `healthlens.db` to `.gitignore` so the local database is not committed.

## IntelliJ steps

1. Open this folder as a Maven project.
2. Let IntelliJ import/reload `pom.xml`.
3. Use **Maven > Reload All Maven Projects** if dependencies are not detected automatically.
4. Make sure the project uses a JDK compatible with the project's Java version settings.
5. Run the Maven goal `javafx:run`, or run `com.healthlens.Main`.
6. On first run, SQLite automatically creates `healthlens.db` in the project's working directory.
7. Open the Guide and go to Page 6. The Amina/Rafi/Nadia rows are seeded into SQLite the first time and then read from the database.

## If you are replacing an existing local checkout

Do not force-checkout the old SQLite branch over uncommitted work. This branch already contains the integration.

After copying this project over your existing checkout, verify:

```bash
git status
```

Then push the prepared branch with:

```bash
git push -u origin feature/healthlens-v2-sqlite
```

## If Maven reports stale build output

Use:

```bash
mvn clean javafx:run
```

or in IntelliJ use **Maven > Lifecycle > clean**, then run `javafx:run` again.
