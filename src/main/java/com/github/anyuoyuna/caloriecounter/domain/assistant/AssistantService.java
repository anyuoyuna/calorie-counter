package com.github.anyuoyuna.caloriecounter.domain.assistant;

import com.github.anyuoyuna.caloriecounter.dto.AssistantIntent;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
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
            - QUESTION: если пользователь задает вопрос о питании или здоровье.
            - GREETING: если это просто приветствие или пустой разговор.
            - UNKNOWN: если вообще непонятно, что хочет пользователь.
            
            Если в одном сообщении и еда, и активность — выбери наиболее важное как primaryIntent, но перечисли оба в списке actions.
            
            Верни ответ строго в формате JSON:
            {
              "primaryIntent": "ИНТЕНТ",
              "actions": ["FOOD", "ACTIVITY"]
            }
            """;

    public AssistantService(GoogleAiGeminiChatModel chatModel) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatLanguageModel(chatModel)
                .build();
    }


    public AssistantIntent analyze(String text) {
        try {
            // 3. Просто вызываем метод, библиотека сама сделает запрос и распарсит JSON!
            return classifier.classify(text);
        } catch (Exception e) {
            // Если LangChain4j упадет, вернем дефолт
            return new AssistantIntent("UNKNOWN", List.of());
        }
    }
}
