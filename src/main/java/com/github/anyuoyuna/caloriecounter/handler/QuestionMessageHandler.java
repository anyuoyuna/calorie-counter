package com.github.anyuoyuna.caloriecounter.handler;

import com.github.anyuoyuna.caloriecounter.bot.BotResponse;
import com.github.anyuoyuna.caloriecounter.service.GeminiClient;
import org.springframework.stereotype.Component;

@Component
public class QuestionMessageHandler {

    private static final String PROMPT_TEMPLATE = """
            Ты — дружелюбный ассистент по питанию и здоровью. Ответь кратко и по делу на вопрос пользователя.
            Не используй markdown-разметку, простой текст.

            Вопрос: "%s"
            """;

    private final GeminiClient geminiClient;

    public QuestionMessageHandler(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public BotResponse handle(String text) {
        String answer = geminiClient.generateContent(PROMPT_TEMPLATE.formatted(text));
        if (answer == null) {
            return BotResponse.plain("Не получилось получить ответ, попробуй ещё раз.");
        }
        return BotResponse.plain(answer.trim());
    }
}
