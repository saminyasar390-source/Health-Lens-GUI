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
 * JSON + HTTP service used by the HealthLens dashboard.
 * Jackson handles serialization/deserialization and JsonNode is used when
 * the application needs to inspect an arbitrary online JSON response.
 */
public final class JsonHealthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /** A real public JSON endpoint used by the assignment demonstration. */
    public static final String ONLINE_DEMO_URL = "https://dummyjson.com/products/1";

    private JsonHealthService() { }

    public static String toJson(HealthData healthData) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(healthData);
    }

    public static HealthData fromJson(String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON is empty.");
        }
        return OBJECT_MAPPER.readValue(json, HealthData.class);
    }

    /** Parses a HealthLens response with either a direct object or a data wrapper. */
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

    /** Makes a GET request and returns the raw JSON response. */
    public static String fetchApiResponse(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("API URL is empty.");
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.trim()))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("API returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Live online demonstration: fetches a public JSON endpoint and extracts
     * fields from the arbitrary response using Jackson's JsonNode tree API.
     */
    public static OnlineApiDemoResult runOnlineApiDemo() throws Exception {
        String json = fetchApiResponse(ONLINE_DEMO_URL);
        JsonNode root = OBJECT_MAPPER.readTree(json);
        String id = root.path("id").asText("n/a");
        String title = root.path("title").asText("n/a");
        String category = root.path("category").asText("n/a");
        double price = root.path("price").asDouble(Double.NaN);
        return new OnlineApiDemoResult(ONLINE_DEMO_URL, json, id, title, category, price);
    }


    /**
     * Inspects an API response before deciding whether it can be imported as
     * HealthData. Generic APIs such as DummyJSON are still accepted and shown
     * as JSON rather than causing Jackson to throw an unknown-field exception.
     */
    public static ApiResponseResult inspectApiResponse(String url, String json) throws Exception {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("API response is empty.");
        }

        JsonNode root = OBJECT_MAPPER.readTree(json);
        JsonNode data = root.has("data") && root.get("data").isObject()
                ? root.get("data") : root;

        boolean isHealthData = data.isObject() &&
                (data.has("sleepHours") || data.has("waterGlasses") ||
                 data.has("exerciseMinutes") || data.has("stressLevel"));

        HealthData healthData = isHealthData
                ? OBJECT_MAPPER.treeToValue(data, HealthData.class)
                : null;

        StringBuilder summary = new StringBuilder();
        if (data.isObject()) {
            data.fields().forEachRemaining(entry -> {
                if (summary.length() > 0) summary.append("\n");
                JsonNode value = entry.getValue();
                String display = value.isValueNode() ? value.asText() : value.toString();
                summary.append(entry.getKey()).append(" = ").append(display);
            });
        } else {
            summary.append("JSON root type = ").append(root.getNodeType());
        }

        return new ApiResponseResult(url, json, isHealthData, healthData, summary.toString());
    }

    public static String loadSampleApiResponse() throws Exception {
        String resource = "/com/healthlens/json/sample-health-api-response.json";
        try (InputStream input = JsonHealthService.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Bundled sample JSON file was not found: " + resource);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public record OnlineApiDemoResult(String url, String rawJson, String id, String title, String category, double price) { }

    public record ApiResponseResult(
            String url,
            String rawJson,
            boolean isHealthData,
            HealthData healthData,
            String summary
    ) { }
}
