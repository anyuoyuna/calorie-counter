package com.github.anyuoyuna.caloriecounter.domain.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.dto.ParsedActivity;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.entity.WeightLog;
import com.github.anyuoyuna.caloriecounter.repository.WeightLogRepository;
import com.github.anyuoyuna.caloriecounter.infrastructure.ai.AiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class ActivityParsingService {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final WeightLogRepository weightLogRepo;
    private final Clock clock;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String PROMPT_TEMPLATE = """
            Ты — ассистент по трекингу физической активности. Пользователь описывает тренировку или активность.
            Извлеки данные и оцени расход калорий.

            Сегодняшняя дата: %s
            Вес пользователя: %.1f кг

            Правила:
            1. Если указана дата/время ("вчера", "позавчера") - определи дату в формате YYYY-MM-DD. Если нет - используй сегодняшнюю.
            2. Определи тип активности (activityType) - коротко, на русском (например "теннис", "бег", "силовая тренировка").
            3. Определи длительность в минутах (durationMinutes). Если явно не указана, но есть косвенные признаки - оцени разумно.
            4. Оцени расход калорий (estimatedCaloriesBurned), используя стандартные MET-коэффициенты для этого типа активности и вес пользователя.
               Если пользователь указал точное значение (например, с фитнес-трекера) - используй его вместо своей оценки.
               Если оценить невозможно - верни null.
            5. Верни ТОЛЬКО валидный JSON, без markdown-разметки, без пояснений.

            Формат ответа:
            {
              "date": "YYYY-MM-DD",
              "activityType": "строка",
              "durationMinutes": число или null,
              "estimatedCaloriesBurned": число или null
            }

            Текст от пользователя: "%s"
            """;

    public ActivityParsingService(AiClient aiClient, ObjectMapper objectMapper,
                                  WeightLogRepository weightLogRepo, Clock clock) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.weightLogRepo = weightLogRepo;
        this.clock = clock;
    }

    public ParsedActivity parse(User user, String userText) {
        double weightKg = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user)
                .map(WeightLog::getWeightKg)
                .orElse(70.0);

        String today = LocalDate.now(clock).format(DATE_FMT);
        String prompt = PROMPT_TEMPLATE.formatted(today, weightKg, userText);

        String rawResponse = aiClient.generateContent(prompt);
        if (rawResponse == null) {
            log.warn("Gemini вернула пустой ответ на активность");
            return null;
        }

        try {
            // Читаем напрямую, без stripMarkdownFences
            return objectMapper.readValue(rawResponse, ParsedActivity.class);
        } catch (Exception e) {
            log.error("Ошибка парсинга JSON активности. Ответ: {}", rawResponse, e);
            return null;
        }
    }

}
