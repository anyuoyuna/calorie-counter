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
            1. cleanName: СТРОГО только название продукта в единственном числе (яйцо, сыр, авокадо). БЕЗ цифр, БЕЗ веса, БЕЗ калорий!
            2. originalInput: Текст пользователя как он есть.
            3. ЯЗЫК: Не переводи названия! (пиши "яйцо", а не "egg", "кофе", а не "kava"). Оставляй язык пользователя.
            4. БРЕНДЫ: Если указан бренд (Meiji, Singha, Sponsor, Buldak, Shin Ramen, Gatorade), используй его в cleanName.

            ЛОГИКА ВЕСА (поле grams):
            5. ПРИОРИТЕТ: Явный вес в тексте (124 гр) > Эталоны > Твоя оценка.
            6. ПРАВИЛО 100г (КРИТИЧЕСКИ ВАЖНО): Если пользователь указал КБЖУ (200 ккал и т.д.), но НЕ указал вес — ты ОБЯЗАН поставить grams = 100. Это техническое требование, не оставляй там 0!
            7. ЭТАЛОНЫ: 1 яйцо=50г, 1 тост=30г, 1 тортилья=65г, 1 слайс ветчины/сыра=20г, половина авокадо=50г, чашка кофе/чая=250г.
            8. МНОЖИТЕЛИ: Учитывай "х2" или "3 штуки" (например, "2 яйца" -> grams=100).
            9. ДЕЛЕНИЕ: Учитывай "на двоих", "съела половину". Сначала определи общий вес, потом раздели.
            10. КОРРЕКЦИЯ: Если мясо/овощи указаны "сырыми" — уменьшай вес на 20% (уварка). Если курица с костями — уменьшай еще на 25% (только мясо).
            11. НАПИТКИ: "0.5" или "0.33" — это 500 и 330 мл.

            ЛОГИКА КБЖУ (ПОЛЯ totalCalories, totalProtein, totalFat, totalCarbs, totalFiber):
            12. ИТОГО ЗА ПОРЦИЮ: Указывай значения за ВСЮ указанную порцию (вес в поле grams). НИЧЕГО НЕ ДЕЛИ НА 100!
                Если в 2 яйцах (100г) содержится 155 ккал, пиши grams: 100, totalCalories: 155.
            13. ТАЙСКИЕ УПАКОВКИ: Если пользователь ввел КБЖУ сам (например, "тортик 200 ккал") и вес не ясен — СТАВЬ grams = 100, а КБЖУ пиши ровно те, что в тексте (например, totalCalories: 200).
                Если нутриент указан как "хз" или "не знаю" — оцени его сам для этой порции.
            14. НУЛЕВАЯ КАЛОРИЙНОСТЬ: Для воды и черного чая/кофе ставь 0. Кофе с молоком без сахара ~ 35 ккал на порцию.
            15. НИКАКИХ NULL: Если данных нет, ставь 0.

            ОФОРМЛЕНИЕ:
            16. НЕ ОБЪЕДИНЯЙ: Каждый ингредиент (паста, курица, лук) — отдельный объект в списке items.
            17. ЗАМЕТКИ (notes): Опиши кратко логику расчета.

            Формат ответа:
            {
              "date": "YYYY-MM-DD",
              "meal": "breakfast|lunch|dinner|snack",
              "items": [
                {
                  "cleanName": "яйцо",
                  "originalInput": "2 яйца",
                  "grams": 100,
                  "totalCalories": 155,
                  "totalProtein": 13,
                  "totalFat": 11,
                  "totalCarbs": 1.1,
                  "totalFiber": 0
                }
              ],
              "notes": ["..."]
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

    @SystemMessage("Ты — дружелюбный ассистент по питанию и здоровью. Ответь кратко и по делу на русском языке.")
    String askQuestion(@UserMessage String question);

    @SystemMessage("""
            Ты — дружелюбный ассистент по питанию. Напиши короткое (4-6 предложений) 
            человеческое саммари недели на русском языке на основе предоставленных данных. 
            Тон - дружелюбный, честный. Отметь успехи и то, на что стоит обратить внимание.
            """)
    String getWeeklySummary(@UserMessage String data);

//    @SystemMessage("Ты — дружелюбный ассистент. Отвечай кратко на вопросы.")
//    String chat(@UserMessage String message);
}
