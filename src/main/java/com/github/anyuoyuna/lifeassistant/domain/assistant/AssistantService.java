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
            Ты — диспетчер личного ассистента. Твоя задача — классифицировать сообщение пользователя.
            
            Варианты первичного намерения (primaryIntent):
            - FOOD: если пользователь описывает, что он съел или выпил.
            - ACTIVITY: если пользователь описывает тренировку или физическую активность.
            - FINANCE: любые денежные траты, покупки или доходы (например: "кофе 120 бат", "купила кроссовки", "такси 200").
            - QUESTION: если пользователь задает вопрос о питании или здоровье.
            - GREETING: если это просто приветствие или пустой разговор.
            - UNKNOWN: если вообще непонятно, что хочет пользователь.
            
            Если в одном сообщении и еда, и активность — выбери наиболее важное как primaryIntent, но перечисли оба в списке actions.
            
            Верни ответ строго в формате JSON:
            {
              "primaryIntent": "ИНТЕНТ",
              "actions": ["СПИСОК_ИНТЕНТОВ"]
            }
            """;

    public AssistantService(ChatLanguageModel ollamaChatModel) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(ollamaChatModel)
                .build();
    }

    public AssistantIntent analyze(String text) {
        try {
            log.info("Ollama анализирует интент для: {}", text);
            return classifier.classify(text);
        } catch (Exception e) {
            log.error("Ошибка при анализе намерения через Ollama: {}", e.getMessage());
            return new AssistantIntent("UNKNOWN", List.of());
        }
    }
}
