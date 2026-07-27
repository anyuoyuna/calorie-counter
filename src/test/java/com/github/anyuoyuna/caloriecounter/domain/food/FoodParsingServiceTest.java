package com.github.anyuoyuna.caloriecounter.domain.food;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class FoodParsingServiceTest {

    @Test
    void parsesResponseFromAiClientWithoutCallingGemini() {
        // 1. Создаем фиксированное время для теста (всегда 2026-07-22)
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"));

        // 2. Создаем фейковый AI клиент, который просто возвращает готовую JSON строку
        AiClient fakeAiClient = prompt -> """
                {
                  "date": "2026-07-22",
                  "meal": "breakfast",
                  "items": [{
                    "name": "Greek yogurt",
                    "grams": 150,
                    "calories": 59,
                    "protein": 10,
                    "fat": 0.4,
                    "carbs": 3.6,
                    "fiber": 0
                  }]
                }
                """;

        // 3. Собираем сервис (теперь он снова принимает AiClient, ObjectMapper и Clock)
        FoodParsingService service = new FoodParsingService(fakeAiClient, new ObjectMapper(), fixedClock);

        // 4. Вызываем метод
        ParsedMealResponse result = service.parse("Greek yogurt for breakfast");

        // 5. Проверяем результат
        assertThat(result).isNotNull();
        assertThat(result.getMeal()).isEqualTo("breakfast");
        assertThat(result.getDate()).isEqualTo("2026-07-22");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getName()).isEqualTo("Greek yogurt");
        assertThat(result.getItems().get(0).getGrams()).isEqualTo(150.0);
    }
}
