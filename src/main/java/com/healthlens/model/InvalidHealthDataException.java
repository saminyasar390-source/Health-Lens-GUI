package com.healthlens.model;

/**
 * Checked exception raised when a Person or HealthRecord form fails a
 * business-rule validation (blank required field, out-of-range score,
 * negative health value).
 *
 * This mirrors the Lab 1 Foundations pattern (InvalidScoreException):
 * NumberFormatException stays unchecked and is left to Java for genuine
 * parsing failures (e.g. typing "abc" into a numeric field), while broken
 * business rules are raised as this checked exception so every caller must
 * explicitly catch or declare it rather than letting a bad value pass
 * silently.
 */
public class InvalidHealthDataException extends Exception {
    public InvalidHealthDataException(String message) {
        super(message);
    }
}
