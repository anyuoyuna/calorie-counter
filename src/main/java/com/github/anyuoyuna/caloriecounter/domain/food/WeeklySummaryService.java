package com.github.anyuoyuna.caloriecounter.domain.food;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import com.github.anyuoyuna.caloriecounter.repository.MealEntryRepository;
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
public class WeeklySummaryService {

    private final MealEntryRepository mealEntryRepo;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM");

    // Обновленный промпт: теперь просим вернуть JSON
    private static final String PROMPT_TEMPLATE = """
            Ты — дружелюбный ассистент по питанию. Вот данные пользователя за последние 7 дней:
            цель и факт по калориям и БЖУ за каждый день.

            %s

            Напиши короткое (4-6 предложений) человеческое саммари недели на русском языке.
            Тон - дружелюбный, честный. Отметь успехи и то, на что стоит обратить внимание.
            
            Ответ верни строго в формате JSON:
            {"summary": "текст твоего ответа"}
            """;

    public WeeklySummaryService(MealEntryRepository mealEntryRepo,
                                AiClient aiClient,
                                ObjectMapper objectMapper,
                                Clock clock) {
        this.mealEntryRepo = mealEntryRepo;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public record DaySummary(LocalDate date, double calories, double protein, double fat, double carbs, double fiber) {}

    public String buildWeeklySummary(User user) {
        LocalDate today = LocalDate.now(clock); // Используем наш Clock
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

        String prompt = PROMPT_TEMPLATE.formatted(dataBlock.toString());
        String rawResponse = aiClient.generateContent(prompt);

        if (rawResponse == null) {
            return "Не удалось получить ответ от ИИ.";
        }

        try {
            // Парсим JSON и достаем поле "summary"
            JsonNode root = objectMapper.readTree(rawResponse);
            return root.get("summary").asText();
        } catch (Exception e) {
            log.error("Ошибка парсинга недельного саммари. Ответ был: {}", rawResponse, e);
            return "Не удалось сформировать красивый отчет, но данные сохранены.";
        }
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
                "%s: факт %.0f/%d ккал, Б%.0f/%.0f г, Ж%.0f/%.0f г, У%.0f/%.0f г",
                s.date().format(DATE_FMT),
                s.calories(), user.getDailyCalorieGoal(),
                s.protein(), user.getDailyProteinGoal(),
                s.fat(), user.getDailyFatGoal(),
                s.carbs(), user.getDailyCarbsGoal()
        );
    }
}