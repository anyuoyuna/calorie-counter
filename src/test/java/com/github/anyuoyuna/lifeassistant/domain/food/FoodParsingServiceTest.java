package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.dto.ParsedActivity;
import com.github.anyuoyuna.lifeassistant.dto.ParsedExpense;
import com.github.anyuoyuna.lifeassistant.dto.ParsedFoodItem;
import com.github.anyuoyuna.lifeassistant.dto.ParsedMealResponse;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import dev.langchain4j.data.image.Image;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FoodParsingServiceTest {

    @Test
    void shouldReturnParsedMealResponseUsingGeneralAiAssistant() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneId.of("UTC"));
        GeneralAiAssistant fakeAssistant = new GeneralAiAssistant() {
            @Override
            public ParsedMealResponse parseFood(String text, String today) {
                ParsedMealResponse response = new ParsedMealResponse();
                response.setDate(today);
                response.setMeal("breakfast");
                ParsedFoodItem item = new ParsedFoodItem();
                item.setCleanName("egg");
                item.setOriginalInput("2 eggs");
                item.setGrams(100.0);
                item.setTotalCalories(155.0);
                item.setTotalProtein(13.0);
                item.setTotalFat(11.0);
                item.setTotalCarbs(1.1);
                item.setTotalFiber(0.0);
                response.setItems(List.of(item));
                return response;
            }

            @Override
            public String askQuestion(String question) { return null; }

            @Override
            public String getWeeklySummary(String data) { return null; }

            @Override
            public String parseReceipt(Image image) {
                return null;
            }

            @Override
            public ParsedActivity parseActivity(String text, double weight, String today) { return null; }

            @Override
            public ParsedExpense parseExpense(String text) { return null; }
        };

        FoodParsingService service = new FoodParsingService(fakeAssistant, fixedClock);
        ParsedMealResponse result = service.parse("2 eggs for breakfast");
        assertThat(result).isNotNull();
        assertThat(result.getDate()).isEqualTo("2026-07-22");
        assertThat(result.getItems()).hasSize(1);
        ParsedFoodItem firstItem = result.getItems().get(0);
        assertThat(firstItem.getCleanName()).isEqualTo("egg");
        assertThat(firstItem.getTotalCalories()).isEqualTo(155.0);
        assertThat(firstItem.getGrams()).isEqualTo(100.0);
    }
}