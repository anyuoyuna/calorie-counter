package com.github.anyuoyuna.lifeassistant.bot;

import com.github.anyuoyuna.lifeassistant.domain.assistant.AssistantService;
import com.github.anyuoyuna.lifeassistant.domain.assistant.UndoService;
import com.github.anyuoyuna.lifeassistant.domain.finance.FinanceService;
import com.github.anyuoyuna.lifeassistant.dto.AssistantIntent;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.exception.GeminiUnavailableException;
import com.github.anyuoyuna.lifeassistant.handler.FinanceMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.github.anyuoyuna.lifeassistant.bot.MenuKeyboard.BTN_QUESTION;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageRouter {

    private final FinanceMessageHandler financeMessageHandler;
    private final AssistantService assistantService;
    private final FinanceService financeService;
    private final UndoService undoService;

    public BotResponse route(User user, String text) {
        if (text.equalsIgnoreCase("/undo")) {
            return BotResponse.plain(undoService.undoLastAction(user));
        }
        if (text.equals("/start")) {
            return BotResponse.plainWithMenu("Welcome back! Send an expense, income, receipt, or finance question.");
        }
        if (text.equals(BTN_QUESTION)) {
            return BotResponse.plain("Ask a question about your finances.");
        }
        if (text.equals("/import_history")) {
            int count = financeService.importHistoryFromSheets();
            return BotResponse.plain("Import complete! Loaded rows: " + count);
        }
        if (text.equals("/ping")) {
            return BotResponse.plain("I am alive and working on Render!");
        }
        AssistantIntent intent;
        try {
            intent = assistantService.analyze(text);
        } catch (GeminiUnavailableException e) {
            log.warn("Gemini is unavailable while classifying a financial message", e);
            return BotResponse.plain("AI service is temporarily unavailable. Please try again in a minute.");
        }
        return switch (intent.primaryIntent()) {
            case "FINANCE_ADD" -> financeMessageHandler.handle(user, text);
            case "FINANCE_QUERY" -> financeMessageHandler.handleQuery(user, text);
            case "GREETING" -> BotResponse.plainWithMenu("Hi! Send an expense, income, receipt, or finance question.");
            default -> {
                log.warn("Failed to determine financial intent for text: {}", text);
                yield BotResponse.plain("I didn't quite catch that. Send an expense, income, or finance question.");
            }
        };
    }
}
