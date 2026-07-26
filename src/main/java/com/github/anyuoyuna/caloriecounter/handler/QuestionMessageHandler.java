package com.github.anyuoyuna.caloriecounter.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.bot.BotResponse;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuestionMessageHandler {

    private final ObjectMapper objectMapper;

    private static final String PROMPT_TEMPLATE = """
        Ты — дружелюбный ассистент по питанию. Ответь кратко на вопрос.
        Верни ответ строго в формате JSON: {"answer": "текст твоего ответа"}
        
        Вопрос: "%s"
        """;

    private final AiClient aiClient;

    public QuestionMessageHandler(ObjectMapper objectMapper, AiClient aiClient) {
        this.objectMapper = objectMapper;
        this.aiClient = aiClient;
    }

    public BotResponse handle(String text) {
        String response = aiClient.generateContent(PROMPT_TEMPLATE.formatted(text));
        try {
            // Извлекаем текст из JSON
            JsonNode node = objectMapper.readTree(response);
            return BotResponse.plain(node.get("answer").asText());
        } catch (Exception e) {
            log.error("Ошибка при получении ответа на вопрос: {}", response, e);
            return BotResponse.plain("Не удалось получить ответ.");
        }
    }
}
