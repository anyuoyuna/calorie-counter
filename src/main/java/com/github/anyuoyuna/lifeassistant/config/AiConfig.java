package com.github.anyuoyuna.lifeassistant.config;

import com.github.anyuoyuna.lifeassistant.bot.LifeAssistantBot;
import com.github.anyuoyuna.lifeassistant.domain.assistant.AssistantService;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class AiConfig {

    @Value("${gemini.chat-model}")
    private String geminiModelName;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Bean
    public EmbeddingModel embeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-embedding-001")
                .build();
    }

    @Bean("geminiModel")
    public ChatLanguageModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .temperature(0.0)
                .build();
    }

    @Bean
    public AssistantService assistantService() {
        return new AssistantService(geminiChatModel());
    }

    @Bean
    public GeneralAiAssistant generalAiAssistant() {
        return AiServices.builder(GeneralAiAssistant.class)
                .chatLanguageModel(geminiChatModel())
                .build();
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(LifeAssistantBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}