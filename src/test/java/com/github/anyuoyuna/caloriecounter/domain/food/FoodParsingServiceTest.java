package com.github.anyuoyuna.caloriecounter.domain.food;

import com.github.anyuoyuna.caloriecounter.dto.ParsedActivity;
import com.github.anyuoyuna.caloriecounter.dto.ParsedExpense;
import com.github.anyuoyuna.caloriecounter.dto.ParsedFoodItem;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.GeneralAiAssistant;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodParsingServiceTest {

    @Test
    void shouldReturnParsedMealResponseUsingGeneralAiAssistant() {
        // 1. Создаем фиксированное время
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"));

        // 2. Создаем заглушку (Fake) нашего ассистента
        GeneralAiAssistant fakeAssistant = new GeneralAiAssistant() {
            @Override
            public ParsedMealResponse parseFood(String text, String today) {
                ParsedMealResponse response = new ParsedMealResponse();
                response.setDate(today);
                response.setMeal("breakfast");

                ParsedFoodItem item = new ParsedFoodItem();
                item.setCleanName("яйцо");
                item.setOriginalInput("2 яйца");
                item.setGrams(100.0);
                item.setTotalCalories(155.0);
                item.setTotalProtein(13.0);
                item.setTotalFat(11.0);
                item.setTotalCarbs(1.1);
                item.setTotalFiber(0.0);

                response.setItems(List.of(item));
                return response;
            }

            // РЕАЛИЗУЕМ НОВЫЕ МЕТОДЫ ИНТЕРФЕЙСА (заглушки)
            @Override
            public String askQuestion(String question) { return null; }

            @Override
            public String getWeeklySummary(String data) { return null; }

            @Override
            public ParsedActivity parseActivity(String text, double weight, String today) { return null; }

            @Override
            public ParsedExpense parseExpense(String text) { return null; }
        };

        // 3. Инициализируем сервис
        FoodParsingService service = new FoodParsingService(fakeAssistant, fixedClock);

        // 4. Выполняем тест
        ParsedMealResponse result = service.parse("2 яйца на завтрак");

        // 5. Проверяем
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo("2026-07-22");
        assertThat(result.getItems()).hasSize(1);

        ParsedFoodItem firstItem = result.getItems().get(0);
        assertThat(firstItem.getCleanName()).isEqualTo("яйцо");
        assertThat(firstItem.getTotalCalories()).isEqualTo(155.0);
        assertThat(firstItem.getGrams()).isEqualTo(100.0);
    }
}