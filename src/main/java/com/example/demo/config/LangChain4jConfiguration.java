package com.example.demo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfiguration {

    @Value("${llm.gemini.api-key:demo-key}")
    private String geminiApiKey;

    @Value("${llm.gemini.model:gemini-flash-latest}")
    private String geminiModel;

    @Value("${llm.gemini.timeout-seconds:30}")
    private Integer timeoutSeconds;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return new GeminiChatLanguageModel(geminiApiKey, geminiModel, timeoutSeconds);
    }
}
