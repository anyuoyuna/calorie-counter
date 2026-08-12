package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.dto.ParsedFoodItem;
import com.github.anyuoyuna.lifeassistant.dto.ParsedMealResponse;
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

    public record ValidationResult(List<ParsedFoodItem> acceptedItems, List<String> rejectedItemNames) {
    }

    private boolean isValid(ParsedFoodItem item) {
        String displayName = displayName(item);
        if (item == null || item.getOriginalInput() == null || item.getOriginalInput().isBlank()) {
            log.warn("Validator: product name is empty");
            return false;
        }
        if (item.getGrams() == null || item.getGrams() < 0) {
            log.warn("Validator: product '{}' has invalid weight (null or < 0)", displayName);
            return false;
        }
        if (item.getGrams() > MAX_GRAMS_PER_ITEM) {
            log.warn("Validator: product '{}' weight is too high", displayName);
            return false;
        }
        if (item.getTotalCalories() == null || item.getTotalCalories() < 0 || item.getTotalCalories() > MAX_CALORIES_PER_100_GRAMS * 5) {
            log.warn("Validator: product '{}' has suspicious calorie count", displayName);
            return false;
        }
        if (!isMacroValid(item.getOriginalInput(), "Proteins", item.getTotalProtein()) ||
                !isMacroValid(item.getOriginalInput(), "Fats", item.getTotalFat()) ||
                !isMacroValid(item.getOriginalInput(), "Carbs", item.getTotalCarbs())) {
            return false;
        }
        return true;
    }

    private boolean isMacroValid(String itemName, String macroName, Double value) {
        if (value == null) {
            log.warn("Validator: product '{}' {} is null. Skipping.", itemName, macroName);
            return false;
        }
        if (value < 0 || value > 100) {
            log.warn("Validator: product '{}' has invalid value for {}: {} (must be 0..100)",
                    itemName, macroName, value);
            return false;
        }
        return true;
    }

    private String displayName(ParsedFoodItem item) {
        return item != null && item.getOriginalInput() != null && !item.getOriginalInput().isBlank()
                ? item.getOriginalInput()
                : "unnamed item";
    }
}
