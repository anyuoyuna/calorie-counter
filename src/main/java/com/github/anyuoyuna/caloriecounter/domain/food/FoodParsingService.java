package com.github.anyuoyuna.caloriecounter.domain.food;

import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.GeneralAiAssistant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
public class FoodParsingService {

    private final GeneralAiAssistant aiAssistant; // Используем новый интерфейс
    private final Clock clock;

    public FoodParsingService(GeneralAiAssistant aiAssistant, Clock clock) {
        this.aiAssistant = aiAssistant;
        this.clock = clock;
    }

    public ParsedMealResponse parse(String userText) {
        String today = LocalDate.now(clock).toString();

        log.info("Парсинг еды через GeneralAiAssistant: {}", userText);

        // Магия LangChain4j: метод вернет сразу ГОТОВЫЙ объект
        ParsedMealResponse response = aiAssistant.parseFood(userText, today);

        if (response != null && response.getItems() != null) {
            log.info("Ollama выдала позиций: {}. Первая позиция: {}",
                    response.getItems().size(),
                    response.getItems().isEmpty() ? "пусто" : response.getItems().get(0).getOriginalInput());
        } else {
            log.warn("Ollama вернула пустой или некорректный объект ParsedMealResponse");
        }

        return response;
    }
}
