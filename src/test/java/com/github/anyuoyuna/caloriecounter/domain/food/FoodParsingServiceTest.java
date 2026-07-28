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
                // Имитируем успешный разбор 2 яиц
                ParsedMealResponse response = new ParsedMealResponse();
                response.setDate(today);
                response.setMeal("breakfast");

                ParsedFoodItem item = new ParsedFoodItem();
                item.setCleanName("яйцо");
                item.setOriginalInput("2 яйца");
                item.setGrams(100.0);
                item.setCalories(155.0);
                item.setProtein(13.0);
                item.setFat(11.0);
                item.setCarbs(1.1);
                item.setFiber(0.0);

                response.setItems(List.of(item));
                return response;
            }

            // Эти методы в тесте еды нам не нужны, оставляем пустыми (stubs)
            @Override public ParsedActivity parseActivity(String text, double weight, String today) { return null; }
            @Override public ParsedExpense parseExpense(String text) { return null; }
            @Override public String chat(String message) { return null; }
        };

        // 3. Инициализируем сервис (теперь только ассистент и часы!)
        FoodParsingService service = new FoodParsingService(fakeAssistant, fixedClock);

        // 4. Выполняем тест
        ParsedMealResponse result = service.parse("2 яйца на завтрак");

        // 5. Проверяем
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo("2026-07-22");
        assertThat(result.getItems()).hasSize(1);

        ParsedFoodItem firstItem = result.getItems().get(0);
        assertThat(firstItem.getCleanName()).isEqualTo("яйцо");
        assertThat(firstItem.getOriginalInput()).isEqualTo("2 яйца");
        assertThat(firstItem.getGrams()).isEqualTo(100.0);
    }
}