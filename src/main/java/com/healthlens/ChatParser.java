package com.healthlens;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts structured health values (sleep, water, exercise, stress, mood)
 * from a free-text sentence typed into the chat assistant.
 *
 * This is a rule-based (regex/keyword) parser, not a machine-learning model.
 * It's deliberately simple and dependency-free so it always works offline,
 * with zero setup and zero API keys.
 *
 * WANT TO SWAP THIS FOR A REAL AI MODEL LATER?
 * Replace the body of parse() with an HTTP call to the HuggingFace Inference
 * API (or any other LLM endpoint), prompting the model to reply with JSON
 * shaped like:
 *   {"sleepHours":8, "waterGlasses":5, "exerciseMinutes":20, "stressLevel":6, "mood":"Stressed"}
 * then parse that JSON into a ChatUpdate the same way this class does. The
 * rest of HealthLensController (which calls ChatParser.parse(...)) would
 * not need to change at all.
 */
public class ChatParser {

    /** Holds whatever the parser managed to find. Any field may be null (= "not mentioned"). */
    public static class ChatUpdate {
        public Double sleepHours;
        public Double waterGlasses;
        public Double exerciseMinutes;
        public Double stressLevel;
        public String mood; // one of: Great, Good, Okay, Low, Stressed

        public boolean isEmpty() {
            return sleepHours == null && waterGlasses == null && exerciseMinutes == null
                    && stressLevel == null && mood == null;
        }
    }

    private static final Pattern SLEEP_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)\\b[^.]*?sleep|slept\\s*(?:for\\s*)?(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)\\b|sleep\\D{0,10}?(\\d+(?:\\.\\d+)?)\\s*(?:hours?|hrs?|h)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern WATER_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*(?:glass(?:es)?|cups?)\\s*(?:of\\s*water)?|water\\D{0,10}?(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EXERCISE_PATTERN = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*min(?:ute)?s?\\s*(?:of)?\\s*(?:exercise|workout|running|jogging|walk(?:ing)?|gym)|" +
            "(?:exercise[d]?|worked out|walked|ran|jogged)\\D{0,10}?(\\d+(?:\\.\\d+)?)\\s*min",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STRESS_NUMBER_PATTERN = Pattern.compile(
            "stress\\D{0,15}?(\\d+(?:\\.\\d+)?)|(\\d+(?:\\.\\d+)?)\\s*(?:out of 10)?\\D{0,10}?stress",
            Pattern.CASE_INSENSITIVE);

    public static ChatUpdate parse(String message) {
        ChatUpdate result = new ChatUpdate();
        if (message == null || message.isBlank()) {
            return result;
        }
        String text = message.toLowerCase();

        result.sleepHours = firstMatch(SLEEP_PATTERN, text);
        result.waterGlasses = firstMatch(WATER_PATTERN, text);
        result.exerciseMinutes = firstMatch(EXERCISE_PATTERN, text);

        Double stressNumber = firstMatch(STRESS_NUMBER_PATTERN, text);
        if (stressNumber != null) {
            result.stressLevel = clamp(stressNumber, 1, 10);
        }

        result.mood = detectMood(text);

        // Keyword fallback for stress if no explicit number was given
        if (result.stressLevel == null) {
            if (containsAny(text, "very stressed", "extremely stressed", "super stressed", "overwhelmed")) {
                result.stressLevel = 9.0;
            } else if (containsAny(text, "stressed", "anxious", "stressful")) {
                result.stressLevel = 7.0;
            } else if (containsAny(text, "relaxed", "calm", "chill", "peaceful")) {
                result.stressLevel = 2.0;
            }
        }

        return result;
    }

    private static String detectMood(String text) {
        if (containsAny(text, "great", "amazing", "fantastic", "awesome")) return "Great";
        if (containsAny(text, "good", "well", "fine", "happy")) return "Good";
        if (containsAny(text, "stressed", "anxious", "overwhelmed")) return "Stressed";
        if (containsAny(text, "low", "sad", "down", "tired", "exhausted", "bad", "terrible", "awful")) return "Low";
        if (containsAny(text, "okay", "ok", "alright", "so-so", "meh")) return "Okay";
        return null;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private static Double firstMatch(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                String group = m.group(i);
                if (group != null) {
                    try {
                        return Double.parseDouble(group);
                    } catch (NumberFormatException ignored) {
                        // try next group
                    }
                }
            }
        }
        return null;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
