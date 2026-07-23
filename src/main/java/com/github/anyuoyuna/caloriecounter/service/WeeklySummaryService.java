package com.github.anyuoyuna.caloriecounter.service;

import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.repository.MealEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WeeklySummaryService {

    private final MealEntryRepository mealEntryRepo;
    private final AiClient aiClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM");

    private static final String PROMPT_TEMPLATE = """
            Ты — дружелюбный ассистент по питанию. Вот данные пользователя за последние 7 дней:
            цель и факт по калориям и БЖУ+клетчатке за каждый день.

            %s

            Напиши короткое (4-6 предложений) человеческое саммари недели на русском языке. Тон - дружелюбный, без осуждения, по-деловому честный.
            Отметь: сколько дней уложилась в норму, были ли заметные выходы за пределы (в плюс или в минус) и в какие дни,
            есть ли что-то, на что стоит обратить внимание (например, стабильно низкая клетчатка или белок).
            Не используй markdown-разметку, простой текст. Не придумывай факты сверх того, что дано в данных.
            """;

    public WeeklySummaryService(MealEntryRepository mealEntryRepo, AiClient aiClient) {
        this.mealEntryRepo = mealEntryRepo;
        this.aiClient = aiClient;
    }

    public record DaySummary(LocalDate date, double calories, double protein, double fat, double carbs, double fiber) {}

    public String buildWeeklySummary(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<MealEntry> entries = mealEntryRepo.findByUserAndEatenAtBetween(
                user, weekStart.atStartOfDay(), today.plusDays(1).atStartOfDay());

        Map<LocalDate, List<MealEntry>> byDay = new LinkedHashMap<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            byDay.put(d, new java.util.ArrayList<>());
        }
        for (MealEntry e : entries) {
            LocalDate day = e.getEatenAt().toLocalDate();
            byDay.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(e);
        }

        StringBuilder dataBlock = new StringBuilder();
        for (Map.Entry<LocalDate, List<MealEntry>> dayEntry : byDay.entrySet()) {
            DaySummary summary = calculateDaySummary(dayEntry.getKey(), dayEntry.getValue());
            dataBlock.append(formatDayLine(summary, user)).append("\n");
        }

        String prompt = PROMPT_TEMPLATE.formatted(dataBlock.toString());

        String narrative = aiClient.generateContent(prompt);
        if (narrative == null) {
            log.warn("Не удалось получить недельное саммари от Gemini для пользователя {}", user.getTelegramId());
            return "Не получилось сформировать саммари недели, попробуй чуть позже.";
        }

        return narrative.trim();
    }

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
                "%s: калории %.0f/%d ккал, белок %.0f/%.0f г, жиры %.0f/%.0f г, углеводы %.0f/%.0f г, клетчатка %.0f/%.0f г",
                s.date().format(DATE_FMT),
                s.calories(), user.getDailyCalorieGoal(),
                s.protein(), user.getDailyProteinGoal(),
                s.fat(), user.getDailyFatGoal(),
                s.carbs(), user.getDailyCarbsGoal(),
                s.fiber(), user.getDailyFiberGoal()
        );
    }
}
