package com.github.anyuoyuna.caloriecounter.config;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {
    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public GoogleAiGeminiChatModel langchainGeminiModel(@Value("${gemini.api.key}") String apiKey) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-flash-latest")
                .logRequestsAndResponses(true)
                .build();
    }
}