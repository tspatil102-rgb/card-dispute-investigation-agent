package com.example.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j Configuration
 * Configures the LLM provider - defaults to Vertex AI, falls back to Gemini if needed
 */
@Configuration
public class LangChain4jConfiguration {

    @Value("${llm.vertexai.project-id:}")
    private String vertexAiProjectId;

    @Value("${llm.vertexai.region:us-central1}")
    private String vertexAiRegion;

    @Value("${llm.vertexai.model:gemini-2.0-flash}")
    private String vertexAiModel;

    @Value("${llm.vertexai.timeout-seconds:30}")
    private Integer vertexAiTimeoutSeconds;

    @Value("${llm.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${llm.gemini.model:gemini-flash-latest}")
    private String geminiModel;

    @Value("${llm.gemini.timeout-seconds:30}")
    private Integer geminiTimeoutSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // Try Vertex AI first if configured
        if (vertexAiProjectId != null && !vertexAiProjectId.isEmpty()) {
            try {
                System.out.println("✅ Initializing Vertex AI ChatLanguageModel (Project: " + vertexAiProjectId + ", Region: " + vertexAiRegion + ")");
                return new VertexAIChatLanguageModel(vertexAiProjectId, vertexAiRegion, vertexAiModel, vertexAiTimeoutSeconds);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to initialize Vertex AI: " + e.getMessage());
                System.err.println("Falling back to Gemini API...");
            }
        }

        // Fallback to Gemini if Vertex AI not configured
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            System.out.println("✅ Initializing Gemini ChatLanguageModel");
            return new GeminiChatLanguageModel(geminiApiKey, geminiModel, geminiTimeoutSeconds);
        }

        // If neither is configured, return a Vertex AI instance anyway (will initialize with ADC)
        System.out.println("⚠️ No LLM provider explicitly configured, attempting to use Application Default Credentials for Vertex AI");
        return new VertexAIChatLanguageModel(vertexAiProjectId, vertexAiRegion, vertexAiModel, vertexAiTimeoutSeconds);
    }
}
