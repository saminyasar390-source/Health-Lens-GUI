package com.healthlens.json;

import com.healthlens.model.HealthData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Handles JSON serialization, parsing, and API response processing.
 *
 * Week 7 concepts demonstrated here:
 *  1. Java object -> JSON string (serialization)
 *  2. JSON string -> Java object (deserialization)
 *  3. Parsing a structured API response
 *  4. HTTP GET request + response handling
 *
 * The controller does not contain JSON parsing code. That keeps the
 * Controller focused on JavaFX/UI work and makes this class reusable.
 */
public final class JsonHealthService {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private JsonHealthService() {
        // Utility class
    }

    /** Converts the current HealthData model into readable JSON. */
    public static String toJson(HealthData healthData) {
        return GSON.toJson(healthData);
    }

    /** Converts JSON back into a HealthData Java object. */
    public static HealthData fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON is empty.");
        }
        return GSON.fromJson(json, HealthData.class);
    }

    /**
     * Parses either:
     *
     *   { "sleepHours": 8, ... }
     *
     * or an API-style response:
     *
     *   { "status": "success", "data": { "sleepHours": 8, ... } }
     */
    public static HealthData parseApiResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("API response is empty.");
        }

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement data = root.has("data") ? root.get("data") : root;

        if (data == null || !data.isJsonObject()) {
            throw new IllegalArgumentException("API response does not contain a JSON object.");
        }

        return GSON.fromJson(data, HealthData.class);
    }

    /**
     * Makes a GET request to an API URL and returns its JSON response.
     * This method is intended to be called from a background thread so
     * the JavaFX Application Thread never freezes while waiting for HTTP.
     */
    public static String fetchApiResponse(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("API URL is empty.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "API returned HTTP " + response.statusCode()
            );
        }

        return response.body();
    }
}
