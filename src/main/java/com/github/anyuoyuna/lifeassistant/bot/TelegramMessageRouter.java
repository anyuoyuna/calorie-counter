package com.github.anyuoyuna.lifeassistant.bot;

import com.github.anyuoyuna.lifeassistant.domain.assistant.AssistantService;
import com.github.anyuoyuna.lifeassistant.domain.assistant.UndoService;
import com.github.anyuoyuna.lifeassistant.domain.finance.FinanceService;
import com.github.anyuoyuna.lifeassistant.domain.food.DailyReportService;
import com.github.anyuoyuna.lifeassistant.domain.food.WeeklySummaryService;
import com.github.anyuoyuna.lifeassistant.domain.profile.ProfileService;
import com.github.anyuoyuna.lifeassistant.dto.AssistantIntent;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.handler.ActivityMessageHandler;
import com.github.anyuoyuna.lifeassistant.handler.FinanceMessageHandler;
import com.github.anyuoyuna.lifeassistant.handler.FoodMessageHandler;
import com.github.anyuoyuna.lifeassistant.handler.QuestionMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

import static com.github.anyuoyuna.lifeassistant.bot.MenuKeyboard.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageRouter {

    private final ProfileService profileService;
    private final WeeklySummaryService weeklySummaryService;
    private final DailyReportService dailyReportService;
    private final FoodMessageHandler foodMessageHandler;
    private final ActivityMessageHandler activityMessageHandler;
    private final QuestionMessageHandler questionMessageHandler;
    private final FinanceMessageHandler financeMessageHandler;
    private final AssistantService assistantService;
    private final Clock clock;
    private final FinanceService financeService;
    private final UndoService undoService;

    public BotResponse route(User user, String text) {

        if (text.equalsIgnoreCase("/undo")) {
            log.info("User {} requested action cancellation", user.getDisplayName());
            String message = undoService.undoLastAction(user);
            return BotResponse.plain(message);
        }
        if (text.equals("/start")) {
            return BotResponse.plainWithMenu("Welcome back! Just tell me what you ate or how you worked out.");
        }
        if (text.equals(BTN_TODAY) || text.equals("/today")) {
            return BotResponse.htmlWithMenu(dailyReportService.buildDailySummary(user, LocalDate.now(clock)));
        }
        if (text.equals(BTN_PROFILE) || text.equals("/profile")) {
            return BotResponse.plain(profileService.buildProfileSummary(user));
        }
        if (text.equals(BTN_WEEK) || text.equals("/week")) {
            return BotResponse.plainWithMenu(weeklySummaryService.buildWeeklySummary(user));
        }
        if (text.equals(BTN_FOOD)) {
            return BotResponse.plain("Just write what you ate (e.g., 2-egg omelet and coffee):");
        }
        if (text.equals(BTN_ACTIVITY)) {
            return BotResponse.plain("Describe your activity (e.g., ran for 30 minutes):");
        }
        if (text.equals(BTN_QUESTION)) {
            return BotResponse.plain("Ask question about your finance:");
        }
        if (text.equals("/import_history")) {
            int count = financeService.importHistoryFromSheets();
            return BotResponse.plain("Import complete! Loaded rows: " + count);
        }
        if (text.equals("/ping")) {
            return BotResponse.plain("I am alive and working on Render!");
        }
        AssistantIntent intent = assistantService.analyze(text);
        return switch (intent.primaryIntent()) {
            case "FOOD" -> foodMessageHandler.handle(user, text);
            case "ACTIVITY" -> activityMessageHandler.handle(user, text);
            case "FINANCE" -> financeMessageHandler.handle(user, text);
            case "QUESTION" -> questionMessageHandler.handle(text);
            case "GREETING" -> BotResponse.plainWithMenu("Hey! I'm here. What are we logging today?");
            default -> {
                log.warn("Failed to determine intent for text: {}", text);
                yield BotResponse.plain("I didn't quite catch that. Do you want to log food, a workout, or an expense?");
            }
        };
    }
}