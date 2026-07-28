package com.github.anyuoyuna.caloriecounter.domain.food;

import com.github.anyuoyuna.caloriecounter.dto.ParsedFoodItem;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MealInputValidatorTest {

    private final MealInputValidator validator = new MealInputValidator();

    @Test
    void acceptsValidRecognizedAndUnrecognizedItems() {
        ParsedFoodItem recognized = food("Greek yogurt");
        recognized.setGrams(150.0);
        recognized.setTotalCalories(59.0);
        recognized.setTotalProtein(10.0);
        recognized.setTotalFat(0.4);
        recognized.setTotalCarbs(3.6);
        recognized.setTotalFiber(0.0);

        ParsedFoodItem unrecognized = food("mystery dish");

        MealInputValidator.ValidationResult result = validator.validate(mealWith(recognized, unrecognized));

        assertThat(result.acceptedItems()).containsExactly(recognized, unrecognized);
        assertThat(result.rejectedItemNames()).isEmpty();
    }

    @Test
    void rejectsItemsWithUnsafeValuesOrMissingName() {
        ParsedFoodItem negativeWeight = food("apple");
        negativeWeight.setGrams(-10.0);

        ParsedFoodItem impossibleCalories = food("energy bar");
        impossibleCalories.setGrams(50.0);
        impossibleCalories.setTotalCalories(10_000.0);

        ParsedFoodItem missingName = food(" ");
        missingName.setGrams(100.0);

        MealInputValidator.ValidationResult result = validator.validate(mealWith(negativeWeight, impossibleCalories, missingName));

        assertThat(result.acceptedItems()).isEmpty();
        assertThat(result.rejectedItemNames())
                .containsExactly("apple", "energy bar", "позиция без названия");
    }

    @Test
    void rejectsNonFiniteNumbers() {
        ParsedFoodItem item = food("coffee");
        item.setGrams(Double.NaN);

        MealInputValidator.ValidationResult result = validator.validate(mealWith(item));

        assertThat(result.acceptedItems()).isEmpty();
        assertThat(result.rejectedItemNames()).containsExactly("coffee");
    }

    @Test
    void limitsOneMealToTwentyItems() {
        List<ParsedFoodItem> items = IntStream.rangeClosed(1, 21)
                .mapToObj(index -> food("item-" + index))
                .toList();

        ParsedMealResponse meal = new ParsedMealResponse();
        meal.setItems(items);

        MealInputValidator.ValidationResult result = validator.validate(meal);

        assertThat(result.acceptedItems()).hasSize(20);
        assertThat(result.rejectedItemNames()).containsExactly("item-21");
    }

    private ParsedMealResponse mealWith(ParsedFoodItem... items) {
        ParsedMealResponse meal = new ParsedMealResponse();
        meal.setItems(List.of(items));
        return meal;
    }

    private ParsedFoodItem food(String name) {
        ParsedFoodItem item = new ParsedFoodItem();
        item.setCleanName(name);
        return item;
    }
}
