package com.example.demo.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Vertex AI Chat Language Model Implementation
 * Uses Google Cloud Vertex AI API for generative text responses
 * Supports multi-region fallback and OAuth 2.0 authentication
 */
public class VertexAIChatLanguageModel implements ChatLanguageModel {
    private final String projectId;
    private final String region;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final int timeoutSeconds;
    private final String credentialsFilePath;
    private GoogleCredentials credentials;
    private Long tokenExpireTime = 0L;
    private String cachedAccessToken = null;

    public VertexAIChatLanguageModel(String projectId, String region, String model, int timeoutSeconds, String credentialsFilePath) {
        this.projectId = projectId;
        this.region = region != null && !region.isEmpty() ? region : "us-central1";
        this.model = model != null && !model.isEmpty() ? model : "gemini-2.0-flash";
        this.timeoutSeconds = timeoutSeconds;
        this.credentialsFilePath = credentialsFilePath;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        initializeCredentials();
    }

    private void initializeCredentials() {
        try {
            if (credentialsFilePath != null && !credentialsFilePath.isEmpty()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(credentialsFilePath)) {
                    this.credentials = GoogleCredentials.fromStream(fis)
                            .createScoped("https://www.googleapis.com/auth/cloud-platform");
                    return;
                }
            }

            this.credentials = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        } catch (IOException e) {
            System.err.println("Warning: Failed to initialize Google Cloud credentials: " + e.getMessage());
            System.err.println("Make sure GOOGLE_APPLICATION_CREDENTIALS env var is set or gcloud auth is configured");
        }
    }

    private String getAccessToken() throws IOException {
        if (credentials == null) {
            throw new IOException("Google Cloud credentials not initialized. Set GOOGLE_APPLICATION_CREDENTIALS env var or run: gcloud auth application-default login");
        }

        // Check if cached token is still valid
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        // Refresh credentials and get new token
        credentials.refresh();
        cachedAccessToken = credentials.getAccessToken().getTokenValue();
        tokenExpireTime = System.currentTimeMillis() + (credentials.getAccessToken().getExpirationTime().getTime() - System.currentTimeMillis() - 60000);
        return cachedAccessToken;
    }

    // Getters for testing
    public String getProjectId() {
        return projectId;
    }

    public String getRegion() {
        return region;
    }

    public String getModel() {
        return model;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String prompt = messages.stream()
                .map(ChatMessage::toString)
                .reduce("", (left, right) -> left + (left.isEmpty() ? "" : "\n") + right);
        return Response.from(new AiMessage(generate(prompt)));
    }

    @Override
    public String generate(String prompt) {
        // List of regions to try in fallback order
        String[] regions = {region, "us-central1", "us-west1", "us-east1", "europe-west1", "asia-northeast1"};
        List<String> attemptedRegions = new ArrayList<>();

        for (String currentRegion : regions) {
            if (attemptedRegions.contains(currentRegion)) continue;
            attemptedRegions.add(currentRegion);

            try {
                return callVertexAI(prompt, currentRegion);
            } catch (IOException e) {
                if (attemptedRegions.size() == regions.length) {
                    throw new RuntimeException("Vertex AI request failed after trying all regions: " + e.getMessage(), e);
                }
                System.err.println("Failed to reach Vertex AI in region " + currentRegion + ", trying next region: " + e.getMessage());
                try {
                    Thread.sleep(1000L * attemptedRegions.size());
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Vertex AI request was interrupted: " + e.getMessage(), e);
            } catch (IllegalStateException e) {
                throw e;
            }
        }

        throw new IllegalStateException("Vertex AI unavailable after fallback attempts");
    }

    private String callVertexAI(String prompt, String currentRegion) throws IOException, InterruptedException {
        String accessToken = getAccessToken();

        // Build request body using the Vertex AI generateContent contract
        JsonObject body = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject contentObj = new JsonObject();
        contentObj.addProperty("role", "user");
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        contentObj.add("parts", parts);
        contents.add(contentObj);
        body.add("contents", contents);

        // Build Vertex AI API URL
        String endpoint = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/google/models/%s:generateContent",
                currentRegion, projectId, currentRegion, model
        );

        // Debug: log request body to help diagnose payload mismatches
        try {
            System.out.println("Vertex AI request body: " + gson.toJson(body));
        } catch (Exception ignored) { }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // Handle rate limiting and server errors
        if (response.statusCode() >= 500 || response.statusCode() == 429) {
            throw new IOException("Vertex AI service error: " + response.statusCode() + " " + response.body());
        }

        // Handle client errors
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("Vertex AI API error: " + response.statusCode() + " " + response.body());
        }

        // Parse response
        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        JsonArray candidates = json.getAsJsonArray("candidates");
        if (candidates == null || candidates.size() == 0) {
            throw new IllegalStateException("Vertex AI response missing candidates: " + response.body());
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        // Try the older response shape: content -> parts -> text
        if (candidate.has("content") && candidate.getAsJsonObject("content").has("parts")) {
            JsonObject contentJson = candidate.getAsJsonObject("content");
            JsonArray partsJson = contentJson.getAsJsonArray("parts");
            if (partsJson != null && partsJson.size() > 0) {
                return partsJson.get(0).getAsJsonObject().get("text").getAsString();
            }
        }

        // Newer chat response shape: content -> message -> content -> [{type:text, text:...}]
        if (candidate.has("content") && candidate.getAsJsonObject("content").has("message")) {
            JsonObject messageJson = candidate.getAsJsonObject("content").getAsJsonObject("message");
            if (messageJson.has("content")) {
                JsonArray msgContent = messageJson.getAsJsonArray("content");
                if (msgContent != null && msgContent.size() > 0) {
                    JsonObject first = msgContent.get(0).getAsJsonObject();
                    if (first.has("text")) {
                        return first.get("text").getAsString();
                    }
                }
            }
        }

        throw new IllegalStateException("Vertex AI response missing text content (unsupported shape): " + response.body());
    }
}
