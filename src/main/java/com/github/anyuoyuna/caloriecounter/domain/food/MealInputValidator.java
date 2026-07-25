package com.github.anyuoyuna.caloriecounter.domain.food;

import com.github.anyuoyuna.caloriecounter.dto.ParsedFoodItem;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Treats data received from an LLM as untrusted input before it reaches persistence.
 */
@Component
public class MealInputValidator {

    private static final int MAX_ITEMS_PER_MEAL = 20;
    private static final double MAX_GRAMS_PER_ITEM = 5_000;
    private static final double MAX_CALORIES_PER_100_GRAMS = 1_000;
    private static final double MAX_MACRO_GRAMS_PER_100_GRAMS = 100;

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
        if (item == null || item.getName() == null || item.getName().isBlank()) {
            return false;
        }

        if (!(isInRange(item.getGrams(), 0, MAX_GRAMS_PER_ITEM, true)
                && isInRange(item.getCalories(), 0, MAX_CALORIES_PER_100_GRAMS, true)
                && isInRange(item.getProtein(), 0, MAX_MACRO_GRAMS_PER_100_GRAMS, true)
                && isInRange(item.getFat(), 0, MAX_MACRO_GRAMS_PER_100_GRAMS, true)
                && isInRange(item.getCarbs(), 0, MAX_MACRO_GRAMS_PER_100_GRAMS, true)
                && isInRange(item.getFiber(), 0, MAX_MACRO_GRAMS_PER_100_GRAMS, true))) {
            return false;
        }

        if (!item.isRecognized()) {
            return hasNoNutritionValues(item);
        }

        return item.getProtein() != null && item.getFat() != null && item.getCarbs() != null;
    }

    private boolean isInRange(Double value, double min, double max, boolean nullable) {
        if (value == null) {
            return nullable;
        }
        if (!Double.isFinite(value)) {
            return false;
        }
        return value >= min && value <= max;
    }

    private boolean hasNoNutritionValues(ParsedFoodItem item) {
        return item.getGrams() == null
                && item.getCalories() == null
                && item.getProtein() == null
                && item.getFat() == null
                && item.getCarbs() == null
                && item.getFiber() == null;
    }

    private String displayName(ParsedFoodItem item) {
        return item != null && item.getName() != null && !item.getName().isBlank()
                ? item.getName()
                : "позиция без названия";
    }

    public record ValidationResult(List<ParsedFoodItem> acceptedItems, List<String> rejectedItemNames) {
    }
}
