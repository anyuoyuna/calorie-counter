package com.github.anyuoyuna.caloriecounter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FoodParsingService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String PROMPT_TEMPLATE = """
            Ты — ассистент по подсчёту КБЖУ. Пользователь присылает описание того, что съел или выпил.
                                       Извлеки из текста все продукты/блюда и оцени их КБЖУ.
            
                                       Сегодняшняя дата: %s
            
                                       Правила:
                                       1. Если в тексте есть указание на дату/время приёма пищи ("вчера", "позавчера", "утром", конкретная дата) — определи дату в формате YYYY-MM-DD. Если явного указания нет — используй сегодняшнюю дату.
            
                                       2. Определи тип приёма пищи (meal): "breakfast", "lunch", "dinner" или "snack" — по контексту текста (явное указание или по смыслу). Если невозможно определить — используй "snack".
            
                                       3. Определи вес каждого продукта в граммах:
                                          - Если вес указан в тексте явно в граммах — используй его, weight_source = "explicit".
                                          - Если продукт указан штуками (например, "2 яйца", "половина авокадо", "1 тост") — переведи в граммы по стандартным значениям для этого продукта, weight_source = "estimated".
                                          - Если вес не указан вообще (например, "бокал вина", "кетчуп") — оцени стандартную порцию, weight_source = "estimated".
                                          - Если вес получен через деление/умножение (доли порции, "на двоих" и т.д., см. правило 5) — weight_source = "calculated".
            
                                          КБЖУ (calories, protein, fat, carbs, fiber) указывай в пересчёте на 100 грамм продукта. Поле grams — фактический съеденный вес с учётом всех модификаторов ниже.
            
                                       4. Если продукт указан с множителем "xN" (например, "0.5 Асахи x3", "Singha x2") — умножь количество на N. Итоговый вес — это вес одной порции × N.
            
                                       5. Если в тексте указано количество порций у всего блюда (например, "на двоих", "на троих") — раздели вес каждого ингредиента на это число, чтобы получить твою порцию. Если ПОСЛЕ этого указана ещё доля от твоей порции (например, "съела 3/4") — примени этот множитель к результату предыдущего шага, а не к изначальному весу.
                                          Пример: "на двоих, съела 3/4" + "177 гр лука" → 177 / 2 = 88.5 → 88.5 × 0.75 = 66.4 гр.
                                          Если количество порций у блюда не указано — применяй долю сразу к весу без промежуточного деления.
            
                                       6. Если продукт описан как замороженный/полузамороженный, а вес указан в таком состоянии — учти изменение веса при разморозке/готовке (обычно -15-25%% для мяса и овощей).
            
                                       7. Если объём напитка указан десятичным числом без единиц (например, "0,6 асахи", "0,33 колы") — трактуй как литры, если число в диапазоне 0,25–2,0.
            
                                       8. Если продукт объективно невозможно определить (например, неясное название без контекста) — верни его с calories, protein, fat, carbs, fiber равными null, вместо того чтобы придумывать значения.
            
                                       9. Если в тексте есть уточняющий контекст, который не попадает в другие поля (доли порции, особенности приготовления, откуда продукт) — добавь его как отдельные строки в поле notes.
            
                                       10. Верни ТОЛЬКО валидный JSON, без markdown-разметки, без пояснений до или после.
            
                                       Формат ответа:
                                       {
                                         "date": "YYYY-MM-DD",
                                         "meal": "breakfast|lunch|dinner|snack",
                                         "items": [
                                           {
                                             "name": "название продукта",
                                             "grams": число или null,
                                             "weight_source": "explicit|estimated|calculated",
                                             "calories": число или null (на 100г продукта),
                                             "protein": число или null (на 100г продукта),
                                             "fat": число или null (на 100г продукта),
                                             "carbs": число или null (на 100г продукта),
                                             "fiber": число или null (на 100г продукта)
                                           }
                                         ],
                                         "notes": ["строка контекста", "..."]
                                       }
            
                                       Текст от пользователя: "%s"
            """;

    public FoodParsingService(GeminiClient geminiClient, ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public ParsedMealResponse parse(String userText) {
        String prompt = PROMPT_TEMPLATE.formatted(LocalDate.now().format(DATE_FMT), userText);

        String rawResponse = geminiClient.generateContent(prompt);
        if (rawResponse == null) {
            log.warn("Gemini вернула пустой ответ на текст: {}", userText);
            return null;
        }

        String cleaned = stripMarkdownFences(rawResponse);

        try {
            return objectMapper.readValue(cleaned, ParsedMealResponse.class);
        } catch (Exception e) {
            log.error("Не удалось распарсить JSON от Gemini. Сырой ответ: {}", rawResponse, e);
            return null;
        }
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "").trim();
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
            }
        }
        return trimmed;
    }
}