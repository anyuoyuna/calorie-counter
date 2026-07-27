package com.github.anyuoyuna.caloriecounter.handler;

import com.github.anyuoyuna.caloriecounter.bot.BotResponse;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.exception.GeminiUnavailableException;
import com.github.anyuoyuna.caloriecounter.domain.food.DailyReportService;
import com.github.anyuoyuna.caloriecounter.domain.food.FoodParsingService;
import com.github.anyuoyuna.caloriecounter.domain.food.MealRecordingService;
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
                return BotResponse.plain("Не поняла, что за еда. Попробуй описать подробнее.");
            }
        } catch (GeminiUnavailableException e) {
            log.warn("Gemini недоступна для пользователя {}", user.getTelegramId());
            return BotResponse.plain("Сервис ИИ сейчас перегружен, попробуй отправить сообщение ещё раз через минуту.");
        }

        if (parsed == null || parsed.getItems() == null || parsed.getItems().isEmpty()) {
            return BotResponse.plain("Не поняла, что за еда. Попробуй описать подробнее.");
        }

        MealRecordingService.RecordingResult result = mealRecordingService.recordMeal(user, parsed);
        List<MealEntry> saved = result.savedEntries();

        if (saved.isEmpty()) {
            List<String> skippedItems = new ArrayList<>(result.unrecognizedNames());
            skippedItems.addAll(result.rejectedNames());
            String unrecognized = String.join(", ", skippedItems);
            return BotResponse.plain("Не смогла определить: " + unrecognized + "\nПопробуй описать подробнее или по-другому.");
        }

        LocalDate recordedDate = saved.get(0).getEatenAt().toLocalDate();
        DailyReportService.Macros mealTotals = dailyReportService.calculateMacros(saved);

        String itemNames = saved.stream().map(e -> e.getFoodItem().getName()).collect(Collectors.joining(", "));
        String report = dailyReportService.buildMealReport(itemNames, mealTotals, user, recordedDate);

        if (!result.unrecognizedNames().isEmpty()) {
            report += "\n\n⚠ Не смогла определить: " + String.join(", ", result.unrecognizedNames());
        }
        if (!result.rejectedNames().isEmpty()) {
            report += "\n\n⚠ Не записала некорректные данные: " + String.join(", ", result.rejectedNames());
        }

        return BotResponse.html(report);
    }
}
