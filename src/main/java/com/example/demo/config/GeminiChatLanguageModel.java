package com.example.demo.config;

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
import java.util.List;

public class GeminiChatLanguageModel implements ChatLanguageModel {
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public GeminiChatLanguageModel(String apiKey, String model, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
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
        String[] candidateModels = {model, "gemini-1.5-flash", "gemini-2.0-flash", "gemini-2.5-flash", "gemini-flash-latest"};
        for (int attempt = 0; attempt < candidateModels.length; attempt++) {
            String currentModel = candidateModels[attempt];
            try {
                JsonObject body = new JsonObject();
                JsonArray contents = new JsonArray();
                JsonObject content = new JsonObject();
                JsonArray parts = new JsonArray();
                JsonObject part = new JsonObject();
                part.addProperty("text", prompt);
                parts.add(part);
                content.add("parts", parts);
                contents.add(content);
                body.add("contents", contents);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + currentModel + ":generateContent"))
                        .header("Content-Type", "application/json")
                        .header("X-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                        .timeout(Duration.ofSeconds(60))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 500 || response.statusCode() == 429) {
                    if (attempt < candidateModels.length - 1) {
                        Thread.sleep(1500L * (attempt + 1));
                        continue;
                    }
                    throw new IllegalStateException("Gemini API error: " + response.statusCode() + " " + response.body());
                }
                if (response.statusCode() >= 400) {
                    if (attempt < candidateModels.length - 1) {
                        Thread.sleep(1000L * (attempt + 1));
                        continue;
                    }
                    throw new IllegalStateException("Gemini API error: " + response.statusCode() + " " + response.body());
                }

                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                JsonArray candidates = json.getAsJsonArray("candidates");
                if (candidates == null || candidates.size() == 0) {
                    throw new IllegalStateException("Gemini response missing candidates: " + response.body());
                }

                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject contentJson = candidate.getAsJsonObject("content");
                JsonArray partsJson = contentJson.getAsJsonArray("parts");
                if (partsJson == null || partsJson.size() == 0) {
                    throw new IllegalStateException("Gemini response missing text content: " + response.body());
                }

                return partsJson.get(0).getAsJsonObject().get("text").getAsString();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Gemini request interrupted", e);
            } catch (IOException e) {
                if (attempt < candidateModels.length - 1) {
                    try {
                        Thread.sleep(1500L * (attempt + 1));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                throw new RuntimeException("Gemini request failed", e);
            }
        }
        throw new IllegalStateException("Gemini API unavailable after fallback attempts");
    }
}
