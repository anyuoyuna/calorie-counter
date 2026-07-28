package com.github.anyuoyuna.caloriecounter.domain.food;

import com.github.anyuoyuna.caloriecounter.dto.ParsedFoodItem;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MealInputValidator {

    private static final int MAX_ITEMS_PER_MEAL = 20;
    private static final double MAX_GRAMS_PER_ITEM = 5_000;
    private static final double MAX_CALORIES_PER_100_GRAMS = 1_000;

    public ValidationResult validate(ParsedMealResponse meal) {
        if (meal == null || meal.getItems() == null) {
            return new ValidationResult(List.of(), List.of());
        }

        List<ParsedFoodItem> acceptedItems = new ArrayList<>();
        List<String> rejectedItemNames = new ArrayList<>();

        for (int index = 0; index < meal.getItems().size(); index++) {
            ParsedFoodItem item = meal.getItems().get(index);
            if (index < MAX_ITEMS_PER_MEAL && isValid(item)) {
                acceptedItems.add(item);
            } else {
                rejectedItemNames.add(displayName(item));
            }
        }

        return new ValidationResult(List.copyOf(acceptedItems), List.copyOf(rejectedItemNames));
    }

    private boolean isValid(ParsedFoodItem item) {
        String displayName = displayName(item);

        if (item == null || item.getOriginalInput() == null || item.getOriginalInput().isBlank()) {
            log.warn("Валидатор: пустое имя продукта");
            return false;
        }
        if (item.getGrams() == null || item.getGrams() < 0) {
            log.warn("Валидатор: у продукта '{}' некорректный вес (null или < 0)", displayName);
            return false;
        }
        if (item.getGrams() > MAX_GRAMS_PER_ITEM) {
            log.warn("Валидатор: у продукта '{}' слишком большой вес", displayName);
            return false;
        }
        if (item.getTotalCalories() == null || item.getTotalCalories() < 0 || item.getTotalCalories() > MAX_CALORIES_PER_100_GRAMS * 5) {
            log.warn("Валидатор: у продукта '{}' подозрительные калории", displayName);
            return false;
        }

        if (!isMacroValid(item.getOriginalInput(), "Белки", item.getTotalProtein()) ||
                !isMacroValid(item.getOriginalInput(), "Жиры", item.getTotalFat()) ||
                !isMacroValid(item.getOriginalInput(), "Углеводы", item.getTotalCarbs())) {
            return false;
        }

        return true;
    }

    private boolean isMacroValid(String itemName, String macroName, Double value) {
        if (value == null) {
            log.warn("Валидатор: у продукта '{}' {} = null. Пропускаем.", itemName, macroName);
            return false;
        }
        if (value < 0 || value > 100) {
            log.warn("Валидатор: у продукта '{}' некорректное значение {}: {} (должно быть 0..100)",
                    itemName, macroName, value);
            return false;
        }
        return true;
    }

    private String displayName(ParsedFoodItem item) {
        return item != null && item.getOriginalInput() != null && !item.getOriginalInput().isBlank()
                ? item.getOriginalInput()
                : "позиция без названия";
    }

    public record ValidationResult(List<ParsedFoodItem> acceptedItems, List<String> rejectedItemNames) {
    }
}
