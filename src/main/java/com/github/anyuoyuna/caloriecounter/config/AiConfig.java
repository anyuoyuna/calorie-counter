package com.github.anyuoyuna.caloriecounter.config;

import com.github.anyuoyuna.caloriecounter.domain.assistant.AssistantService;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.GeneralAiAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    @Primary
    public ChatLanguageModel ollamaModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5-coder:7b")
                .timeout(Duration.ofSeconds(60))
                .format("json")
                .build();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("nomic-embed-text")
                .build();
    }

    @Bean
    public ChatLanguageModel geminiModel(@Value("${gemini.api.key}") String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.0)
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean
    public AssistantService assistantService(ChatLanguageModel ollamaModel) {
        return new AssistantService(ollamaModel);
    }

    @Bean
    public GeneralAiAssistant generalAiAssistant(ChatLanguageModel geminiModel) {
        return AiServices.builder(GeneralAiAssistant.class)
                .chatLanguageModel(geminiModel)
                .build();
    }
}