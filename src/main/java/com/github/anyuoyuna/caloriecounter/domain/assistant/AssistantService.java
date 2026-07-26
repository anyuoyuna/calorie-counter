package com.github.anyuoyuna.caloriecounter.domain.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.dto.AssistantIntent;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AssistantService {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

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

    public AssistantService(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public AssistantIntent analyze(String text) {
        String prompt = SYSTEM_PROMPT + "\n\nТекст пользователя: " + text;
        String response = aiClient.generateContent(prompt);

        try {
            return objectMapper.readValue(response, AssistantIntent.class);
        } catch (Exception e) {
            log.error("Ошибка при анализе намерения: {}", response, e);
            return new AssistantIntent("UNKNOWN", null);
        }
    }
}
