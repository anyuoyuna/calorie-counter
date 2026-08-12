package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.dto.ParsedMealResponse;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
public class FoodParsingService {

    private final GeneralAiAssistant aiAssistant;
    private final Clock clock;

    public FoodParsingService(GeneralAiAssistant aiAssistant, Clock clock) {
        this.aiAssistant = aiAssistant;
        this.clock = clock;
    }

    public ParsedMealResponse parse(String userText) {
        String today = LocalDate.now(clock).toString();
        return aiAssistant.parseFood(userText, today);
    }
}
