package com.healthlens.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.healthlens.model.HealthData;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Handles JSON serialization, parsing, and API response processing using Jackson.
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

    /** Jackson's main JSON mapper. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private JsonHealthService() {
        // Utility class
    }

    /** Converts the current HealthData model into readable JSON. */
    public static String toJson(HealthData healthData) throws Exception {
        if (healthData == null) {
            throw new IllegalArgumentException("HealthData is null.");
        }
        return OBJECT_MAPPER.writeValueAsString(healthData);
    }

    /** Converts JSON back into a HealthData Java object. */
    public static HealthData fromJson(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON is empty.");
        }
        return OBJECT_MAPPER.readValue(json, HealthData.class);
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
    public static HealthData parseApiResponse(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("API response is empty.");
        }

        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode data = root.has("data") ? root.get("data") : root;

        if (data == null || !data.isObject()) {
            throw new IllegalArgumentException("API response does not contain a JSON object.");
        }

        return OBJECT_MAPPER.treeToValue(data, HealthData.class);
    }

    /**
     * Loads the bundled sample API response from the application resources.
     * This lets the JSON feature be demonstrated without an internet connection.
     */
    public static String loadSampleApiResponse() throws Exception {
        String resource = "/com/healthlens/json/sample-health-api-response.json";

        try (InputStream input = JsonHealthService.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Bundled sample JSON file was not found: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
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
