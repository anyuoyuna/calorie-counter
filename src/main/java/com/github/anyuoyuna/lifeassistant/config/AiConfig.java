package com.github.anyuoyuna.lifeassistant.config;

import com.github.anyuoyuna.lifeassistant.bot.LifeAssistantBot;
import com.github.anyuoyuna.lifeassistant.domain.assistant.AssistantService;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${ollama.base-url}")
    private String ollamaUrl;

    @Value("${ollama.chat-model}")
    private String ollamaChatModelName;

    @Value("${ollama.embedding-model}")
    private String ollamaEmbeddingModelName;

    @Value("${gemini.chat-model}")
    private String geminiModelName;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean("ollamaModel")
    @Primary
    public ChatLanguageModel ollamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaChatModelName)
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .format("json")
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public OllamaEmbeddingModel embeddingModel() {
        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaEmbeddingModelName)
                .build();
    }

    @Bean("geminiModel")
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .temperature(0.0)
                .logRequestsAndResponses(true)
                .build();
    }

    @Bean
    public AssistantService assistantService(@Qualifier("ollamaModel") ChatLanguageModel model) {
        return new AssistantService(model);
    }

    @Bean
    public GeneralAiAssistant generalAiAssistant(@Qualifier("geminiModel") ChatLanguageModel model) {
        return AiServices.builder(GeneralAiAssistant.class)
                .chatLanguageModel(model)
                .build();
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(LifeAssistantBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}