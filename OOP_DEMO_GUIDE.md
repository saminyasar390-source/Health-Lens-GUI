# OOP demonstration

## Abstraction
`HealthMetric` is an abstract class with the abstract method:
`calculateScore(HealthData data)`.

## Inheritance
These classes extend `HealthMetric`:
- `SleepMetric`
- `WaterMetric`
- `ExerciseMetric`
- `StressMetric`

## Polymorphism
`ScoreCalculator` creates a `HealthMetric[]` and invokes `calculateScore()` through the parent type. Java dispatches to each subclass implementation.

## Interfaces already present
The project also contains listener interfaces such as `SyncService.Listener`, `BreathingSession.Listener`, and `NotificationCenterService.Listener`, plus `GuideController implements Initializable`.
