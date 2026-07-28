package com.github.anyuoyuna.caloriecounter.infrastructure.ai;

import com.github.anyuoyuna.caloriecounter.dto.ParsedActivity;
import com.github.anyuoyuna.caloriecounter.dto.ParsedExpense;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface GeneralAiAssistant {
    String FOOD_PROMPT = """
    Ты — эксперт-нутрициолог и математический парзер. Твоя задача: извлечь данные о еде в JSON.
        Сегодня: {{today}}
    
        ПРАВИЛА ИМЕНОВАНИЯ:
        1. cleanName: СТРОГО только название продукта в единственном числе (яйцо, сыр, авокадо). 
        2. originalInput: Текст пользователя как он есть.
        3. ЯЗЫК: Не переводи названия! (пиши "яйцо", а не "egg", "кофе", а не "kava").
        4. БРЕНДЫ: Если указан бренд (Meiji, Singha, Sponsor, Buldak, Shin Ramen, Gatorade), используй его в cleanName.
    
        ЛОГИКА ВЕСА (поле grams):
        5. ПРИОРИТЕТ: Явный вес в тексте (124 гр) > Эталоны > Твоя оценка.
        6. ЭТАЛОНЫ: 1 яйцо=50г, 1 тост=30г, 1 тортилья=65г, 1 слайс ветчины/сыра=20г, половина авокадо=50г, чашка кофе/чая=250г.
        7. МНОЖИТЕЛИ: Учитывай "х2" или "3 штуки" (например, "2 яйца" -> grams=100).
        8. ДЕЛЕНИЕ: Учитывай "на двоих", "съела половину". Сначала определи общий вес, потом раздели.
        9. КОРРЕКЦИЯ: Если мясо/овощи "сырые" или "замороженные" — уменьшай вес на 15% (уварка).
        10. НАПИТКИ: "0.5" или "0.33" — это 500 и 330 мл.
    
        ЛОГИКА КБЖУ (СТРОГО НА 100 ГРАММ):
        11. ПРИОРИТЕТ ЦИФР ПОЛЬЗОВАТЕЛЯ: Если в тексте есть цифры (200 ккал, 9 жиров), используй их. 
            ВАЖНО: Если цифры даны на всю порцию (например, "тортик 200 ккал" при весе 150г), ты ОБЯЗАН пересчитать их на 100г: (200 / 150) * 100 = 133.
        12. ЗАПРЕТ УМНОЖЕНИЯ: НИКОГДА не умножай КБЖУ на вес самостоятельно! В полях calories, protein, fat, carbs, fiber всегда должны быть значения только за 100 грамм продукта.
        13. НУЛЕВАЯ КАЛОРИЙНОСТЬ: Для воды и черного чая/кофе ставь 0. Кофе с молоком без сахара ~ 15 ккал на 100г.
        14. НИКАКИХ NULL: Если данных нет, ставь 0.
    
        Формат ответа:
        {
          "date": "YYYY-MM-DD",
          "meal": "breakfast|lunch|dinner|snack",
          "items": [
            {
              "cleanName": "яйцо",
              "originalInput": "2 яйца",
              "grams": 100,
              "calories": 155,
              "protein": 13,
              "fat": 11,
              "carbs": 1.1,
              "fiber": 0
            }
          ],
          "notes": ["описание логики расчета"]
        }
    """;

    String ACTIVITY_PROMPT = """
            Ты — ассистент по трекингу физической активности. Пользователь описывает тренировку или активность.
            Извлеки данные и оцени расход калорий.

            Сегодняшняя дата: {{today}}
            Вес пользователя: {{weight}} кг

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
            """;

    String FINANCE_PROMPT = """
        Ты — финансовый ассистент. Твоя задача — извлечь данные о расходах или доходах.
        
        Категории (используй ТОЛЬКО их):
        Income, Utilities, Housing, Transport, Fun, Health, Clothing, Fitness, Groceries, Delivery, Eateries, Others.
        
        Верни JSON:
        {
          "amount": число,
          "category": "одна из списка выше",
          "description": "место (Grab, Lotus, и т.д.) и на что именно",
          "type": "- или +"
        }
        """;

    @SystemMessage(FOOD_PROMPT)
    ParsedMealResponse parseFood(@UserMessage String text, @V("today") String today);

    @SystemMessage(ACTIVITY_PROMPT)
    ParsedActivity parseActivity(@UserMessage String text, @V("weight") double weight, @V("today") String today);

    @SystemMessage(FINANCE_PROMPT)
    ParsedExpense parseExpense(@UserMessage String text);

    @SystemMessage("Ты — дружелюбный ассистент. Отвечай кратко на вопросы.")
    String chat(@UserMessage String message);
}
