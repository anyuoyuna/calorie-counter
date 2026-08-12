package com.github.anyuoyuna.lifeassistant.handler;

import com.github.anyuoyuna.lifeassistant.bot.BotResponse;
import com.github.anyuoyuna.lifeassistant.dto.ParsedMealResponse;
import com.github.anyuoyuna.lifeassistant.entity.MealEntry;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.exception.GeminiUnavailableException;
import com.github.anyuoyuna.lifeassistant.domain.food.DailyReportService;
import com.github.anyuoyuna.lifeassistant.domain.food.FoodParsingService;
import com.github.anyuoyuna.lifeassistant.domain.food.MealRecordingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FoodMessageHandler {

    private final FoodParsingService foodParsingService;
    private final MealRecordingService mealRecordingService;
    private final DailyReportService dailyReportService;

    public FoodMessageHandler(FoodParsingService foodParsingService,
                              MealRecordingService mealRecordingService,
                              DailyReportService dailyReportService) {
        this.foodParsingService = foodParsingService;
        this.mealRecordingService = mealRecordingService;
        this.dailyReportService = dailyReportService;
    }

    public BotResponse handle(User user, String text) {
        ParsedMealResponse parsed;
        try {
            parsed = foodParsingService.parse(text);
            if (parsed == null) {
                return BotResponse.plain("Didn't recognize this food. Try describing it in more detail.");
            }
        } catch (GeminiUnavailableException e) {
            return BotResponse.plain("AI service is currently busy. Please try sending your message again in a minute.");
        }

        if (parsed.getItems() == null || parsed.getItems().isEmpty()) {
            return BotResponse.plain("Didn't recognize this food. Try describing it in more detail.");
        }
        MealRecordingService.RecordingResult result = mealRecordingService.recordMeal(user, parsed);
        List<MealEntry> saved = result.savedEntries();
        if (saved.isEmpty()) {
            List<String> skippedItems = new ArrayList<>(result.unrecognizedNames());
            skippedItems.addAll(result.rejectedNames());
            String unrecognized = String.join(", ", skippedItems);
            return BotResponse.plain("Couldn't identify it: " + unrecognized + "\nTry describing it in more detail or phrasing it differently.");
        }
        LocalDate recordedDate = saved.get(0).getEatenAt().toLocalDate();
        DailyReportService.Macros mealTotals = dailyReportService.calculateMacros(saved);
        String itemNames = saved.stream().map(e -> e.getFoodItem().getName()).collect(Collectors.joining(", "));
        String report = dailyReportService.buildMealReport(itemNames, mealTotals, user, recordedDate);
        if (!result.unrecognizedNames().isEmpty()) {
            report += "\n\n⚠ Couldn't identify it: " + String.join(", ", result.unrecognizedNames());
        }
        if (!result.rejectedNames().isEmpty()) {
            report += "\n\n⚠ Did not save invalid data: " + String.join(", ", result.rejectedNames());
        }
        return BotResponse.html(report);
    }
}
