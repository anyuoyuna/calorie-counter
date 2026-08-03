package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.entity.MealEntry;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.repository.MealEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklySummaryService {

    private final MealEntryRepository mealEntryRepo;
    private final GeneralAiAssistant aiAssistant; // МЕНЯЕМ
    private final Clock clock;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM");

    public String buildWeeklySummary(User user) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.minusDays(6);

        List<MealEntry> entries = mealEntryRepo.findByUserAndEatenAtBetween(
                user, weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<LocalDate, List<MealEntry>> byDay = new LinkedHashMap<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            byDay.put(d, new ArrayList<>());
        }
        for (MealEntry e : entries) {
            LocalDate day = e.getEatenAt().toLocalDate();
            if (byDay.containsKey(day)) {
                byDay.get(day).add(e);
            }
        }

        StringBuilder dataBlock = new StringBuilder();
        for (Map.Entry<LocalDate, List<MealEntry>> dayEntry : byDay.entrySet()) {
            DaySummary summary = calculateDaySummary(dayEntry.getKey(), dayEntry.getValue());
            dataBlock.append(formatDayLine(summary, user)).append("\n");
        }

        String response = aiAssistant.getWeeklySummary(dataBlock.toString());

        return (response != null) ? response.trim() : "Не удалось сформировать отчет.";
    }

    public record DaySummary(LocalDate date, double calories, double protein, double fat, double carbs, double fiber) {}

    private DaySummary calculateDaySummary(LocalDate date, List<MealEntry> entries) {
        double cal = 0, prot = 0, fat = 0, carbs = 0, fiber = 0;
        for (MealEntry e : entries) {
            double ratio = e.getGrams() / 100.0;
            cal += e.getFoodItem().getCalories() * ratio;
            prot += e.getFoodItem().getProtein() * ratio;
            fat += e.getFoodItem().getFat() * ratio;
            carbs += e.getFoodItem().getCarbs() * ratio;
            if (e.getFoodItem().getFiber() != null) {
                fiber += e.getFoodItem().getFiber() * ratio;
            }
        }
        return new DaySummary(date, cal, prot, fat, carbs, fiber);
    }

    private String formatDayLine(DaySummary s, User user) {
        return String.format(
                "%s: факт %.0f/%d ккал, Б%.0f/%.0f г, Ж%.0f/%.0f г, У%.0f/%.0f г",
                s.date().format(DATE_FMT),
                s.calories(), user.getDailyCalorieGoal(),
                s.protein(), user.getDailyProteinGoal(),
                s.fat(), user.getDailyFatGoal(),
                s.carbs(), user.getDailyCarbsGoal()
        );
    }
}