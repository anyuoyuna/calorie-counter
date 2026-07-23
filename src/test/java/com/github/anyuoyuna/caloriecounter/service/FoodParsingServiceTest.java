package com.github.anyuoyuna.caloriecounter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodParsingServiceTest {

    @Test
    void parsesResponseFromAiClientWithoutCallingGemini() {
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
        FoodParsingService service = new FoodParsingService(fakeAiClient, new ObjectMapper());

        ParsedMealResponse result = service.parse("Greek yogurt for breakfast");

        assertThat(result.getMeal()).isEqualTo("breakfast");
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getName()).isEqualTo("Greek yogurt");
            assertThat(item.getGrams()).isEqualTo(150.0);
            assertThat(item.getCalories()).isEqualTo(59.0);
        });
    }
}
