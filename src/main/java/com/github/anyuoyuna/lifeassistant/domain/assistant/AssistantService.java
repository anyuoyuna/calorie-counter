package com.github.anyuoyuna.lifeassistant.domain.assistant;

import com.github.anyuoyuna.lifeassistant.dto.AssistantIntent;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AssistantService {
    interface IntentClassifier {
        @SystemMessage(SYSTEM_PROMPT)
        AssistantIntent classify(@UserMessage String text);
    }

    private static final String SYSTEM_PROMPT = """
            You are a financial assistant dispatcher. Classify the user's message.
            Primary Intent Options (primaryIntent):
            - FINANCE_ADD: a monetary expense, purchase, or income.
            - FINANCE_QUERY: a question about expenses, income, or financial analysis.
            - GREETING: a greeting or small talk.
            - UNKNOWN: the intent is unclear or not financial.
            Return strictly JSON:
            {
                "primaryIntent":"INTENT",
                "actions":["INTENT_1"]
            }
            """;

    private final IntentClassifier classifier;

    public AssistantService(ChatLanguageModel model) {
        classifier = AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(model)
                .build();
    }

    public AssistantIntent analyze(String text) {
        try {
            return classifier.classify(text);
        } catch (Exception e) {
            log.error("Error analyzing intent via LLM: {}", e.getMessage());
            return new AssistantIntent("UNKNOWN", List.of());
        }
    }
}
