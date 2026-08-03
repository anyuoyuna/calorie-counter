package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.entity.ActivityLog;
import com.github.anyuoyuna.lifeassistant.entity.MealEntry;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.repository.ActivityLogRepository;
import com.github.anyuoyuna.lifeassistant.repository.MealEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class DailyReportService {

    private static final double ACTIVITY_EAT_BACK_RATIO = 0.5;

    private final MealEntryRepository mealEntryRepo;
    private final ActivityLogRepository activityLogRepo;
    private final Clock clock;

    public DailyReportService(MealEntryRepository mealEntryRepo, ActivityLogRepository activityLogRepo, Clock clock) {
        this.mealEntryRepo = mealEntryRepo;
        this.activityLogRepo = activityLogRepo;
        this.clock = clock;
    }

    public record Macros(double calories, double protein, double fat, double carbs, double fiber) {}

    public Macros calculateMacros(List<MealEntry> entries) {
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
        return new Macros(cal, prot, fat, carbs, fiber);
    }

    public Macros dayTotals(User user, LocalDate date) {
        List<MealEntry> entries = mealEntryRepo.findByUserAndEatenAtBetween(
                user, date.atStartOfDay(), date.atStartOfDay().plusDays(1));
        return calculateMacros(entries);
    }

    public int activityCaloriesBurned(User user, LocalDate date) {
        List<ActivityLog> activities = activityLogRepo.findByUserAndActivityDate(user, date);
        return activities.stream()
                .mapToInt(a -> a.getEstimatedCaloriesBurned() != null ? a.getEstimatedCaloriesBurned() : 0)
                .sum();
    }

    public Macros remaining(User user, LocalDate date, Macros day) {
        int burned = activityCaloriesBurned(user, date);
        int bonus = (int) Math.round(burned * ACTIVITY_EAT_BACK_RATIO);
        return new Macros(
                (user.getDailyCalorieGoal() + bonus) - day.calories(),
                user.getDailyProteinGoal() - day.protein(),
                user.getDailyFatGoal() - day.fat(),
                user.getDailyCarbsGoal() - day.carbs(),
                user.getDailyFiberGoal() - day.fiber()
        );
    }

    public String buildMealReport(String itemNames, Macros meal, User user, LocalDate date) {
        Macros day = dayTotals(user, date);
        Macros remaining = remaining(user, date, day);
        String dateLabel = date.equals(LocalDate.now(clock)) ? "Сегодня" : "На " + date;

        StringBuilder sb = new StringBuilder();
        sb.append("Записала: ").append(itemNames).append("\n\n");
        sb.append("<pre>");
        sb.append(row("", "Ккал", "Б", "Ж", "У", "Клетч")).append("\n");
        sb.append("-".repeat(42)).append("\n");
        sb.append(dataRow("Приём", meal)).append("\n");
        sb.append(dataRow(dateLabel, day)).append("\n");
        sb.append(dataRow("Осталось", remaining));
        sb.append("</pre>");
        return sb.toString();
    }

    public String buildDailySummary(User user, LocalDate date) {
        Macros day = dayTotals(user, date);
        Macros remaining = remaining(user, date, day);
        String dateLabel = date.equals(LocalDate.now(clock)) ? "Сегодня" : "На " + date;

        StringBuilder sb = new StringBuilder();
        sb.append("<pre>");
        sb.append(row("", "Ккал", "Б", "Ж", "У", "Клетч")).append("\n");
        sb.append("-".repeat(42)).append("\n");
        sb.append(dataRow(dateLabel, day)).append("\n");
        sb.append(dataRow("Осталось", remaining));
        sb.append("</pre>");
        return sb.toString();
    }

    private String row(String label, String kcal, String protein, String fat, String carbs, String fiber) {
        return String.format("%-9s %5s %5s %5s %5s %6s", label, kcal, protein, fat, carbs, fiber);
    }

    private String dataRow(String label, Macros m) {
        return String.format("%-9s %5.0f %5.1f %5.1f %5.1f %6.1f",
                label, m.calories(), m.protein(), m.fat(), m.carbs(), m.fiber());
    }
}
