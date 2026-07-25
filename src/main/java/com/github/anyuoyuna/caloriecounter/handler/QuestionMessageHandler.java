package com.github.anyuoyuna.caloriecounter.handler;

import com.github.anyuoyuna.caloriecounter.bot.BotResponse;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import org.springframework.stereotype.Component;

@Component
public class QuestionMessageHandler {

    private static final String PROMPT_TEMPLATE = """
            Ты — дружелюбный ассистент по питанию и здоровью. Ответь кратко и по делу на вопрос пользователя.
            Не используй markdown-разметку, простой текст.

            Вопрос: "%s"
            """;

    private final AiClient aiClient;

    public QuestionMessageHandler(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public BotResponse handle(String text) {
        String answer = aiClient.generateContent(PROMPT_TEMPLATE.formatted(text));
        if (answer == null) {
            return BotResponse.plain("Не получилось получить ответ, попробуй ещё раз.");
        }
        return BotResponse.plain(answer.trim());
    }
}
