package com.github.anyuoyuna.lifeassistant.handler;

import com.github.anyuoyuna.lifeassistant.bot.BotResponse;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestionMessageHandler {

    private final GeneralAiAssistant aiAssistant;

    public BotResponse handle(String text) {
        String answer = aiAssistant.askQuestion(text);

        if (answer == null || answer.isBlank()) {
            return BotResponse.plain("Couldn't get a response, please try again.");
        }
        return BotResponse.plain(answer.trim());
    }
}
