package com.github.anyuoyuna.lifeassistant.infrastructure.ai;

import com.github.anyuoyuna.lifeassistant.dto.ParsedExpense;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface GeneralAiAssistant {
    String FINANCE_PROMPT = """
            You are a financial assistant. Your task is to extract expense or income data from the user's input into a structured JSON format.
            The user's input may be in English or Russian; understand both languages correctly.
            
            CATEGORIES:
            You MUST use ONLY one of the following categories:
            Income, Utilities, Housing, Transport, Fun, Health, Clothing, Fitness, Groceries, Delivery, Eateries, Others.
            
            EXTRACTION RULES:
            1. amount: The numeric value of the transaction.
            2. category: Strictly one of the allowed strings from the list above.
            3. description: Combine the merchant/location (e.g., Grab, 7-Eleven, Lotus's) and the specific item or purpose.
            4. type: Use "-" for expenses (spending) and "+" for income (earnings).
            5. Category selection priority:
               - If the user explicitly names one of the allowed categories (for example, "Fun", "Utilities", or "Развлечения" when referring to Fun), use that category.
               - Otherwise, if the text explicitly mentions alcohol or an alcoholic drink, classify it as "Fun". This includes beer, wine, пиво, вино, Asahi, Singha, Chang, and similar alcohol names.
               - Otherwise, if the expense is payment of any bill, classify it as "Utilities". This includes insurance, mobile-phone, subscription, and similar bills.
            
            Return strictly JSON format:
            {
              "amount": number,
              "category": "string",
              "description": "merchant/location and purpose",
              "type": "string"
            }
            """;

    String PHOTO_BILL_PROMPT = """
            You are a professional accountant. Analyze the provided receipt image.
            1. Find the total amount spent.
            2. Identify the merchant.
            3. Choose a category: Income, Utilities, Housing, Transport, Fun, Health, Clothing, Fitness, Groceries, Delivery, Eateries, Others.
    
            Return ONLY JSON:
            {
              "amount": (number),
              "category": (string),
              "description": (string),
              "type": "-"
            }
    
            IMPORTANT: Use only data from the image. If you cannot find the amount, return 0.
            """;

    String FINANCE_QUESTION_PROMPT = """
            You are a financial assistant. Answer questions about the user's expenses, income, and spending analytics.
            Use the available finance tools when they help answer the question. Do not provide nutrition or activity advice.
            """;

    @SystemMessage(FINANCE_PROMPT)
    ParsedExpense parseExpense(@UserMessage String text);

    @SystemMessage(FINANCE_QUESTION_PROMPT)
    String askQuestion(@UserMessage String question);
}
