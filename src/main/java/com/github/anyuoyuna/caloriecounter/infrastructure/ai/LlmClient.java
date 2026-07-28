package com.github.anyuoyuna.caloriecounter.infrastructure.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

@Component
public class LlmClient implements AiClient {

    private final ChatLanguageModel chatModel;

    public LlmClient(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String generateContent(String prompt) {
        return chatModel.generate(prompt);
    }
}