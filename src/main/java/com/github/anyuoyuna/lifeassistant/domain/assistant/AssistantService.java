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

    private final IntentClassifier classifier;

    private static final String SYSTEM_PROMPT = """
            You are a personal assistant dispatcher. Your task is to classify the user's message intent.
            
            Primary Intent Options (primaryIntent):
            - FOOD: the user describes what they ate or drank.
            - ACTIVITY: the user describes a workout or physical activity.
            - FINANCE: any monetary expenses, purchases, or income (e.g., "coffee 120 baht", "bought sneakers", "taxi 200").
            - QUESTION: the user asks a general question about nutrition or health.
            - GREETING: a greeting or general small talk.
            - UNKNOWN: the intent is unclear or doesn't fit any category above.
            
            Multi-intent Logic:
            If a message contains multiple intents (e.g., both food and activity), select the most significant one as 'primaryIntent', but list all detected intents in the 'actions' array.
            
            Return the response strictly in JSON format:
            {
              "primaryIntent": "INTENT",
              "actions": ["INTENT_1", "INTENT_2"]
            }
            """;

    public AssistantService(ChatLanguageModel model) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(model)
                .build();
    }

    public AssistantIntent analyze(String text) {
        try {
            log.info("LLM is analyzing intent for: {}", text);
            return classifier.classify(text);
        } catch (Exception e) {
            log.error("Error analyzing intent via LLM: {}", e.getMessage());
            return new AssistantIntent("UNKNOWN", List.of());
        }
    }
}
