package com.github.anyuoyuna.caloriecounter.bot;

import com.github.anyuoyuna.caloriecounter.domain.assistant.AssistantService;
import com.github.anyuoyuna.caloriecounter.domain.finance.FinanceService;
import com.github.anyuoyuna.caloriecounter.domain.food.DailyReportService;
import com.github.anyuoyuna.caloriecounter.domain.food.WeeklySummaryService;
import com.github.anyuoyuna.caloriecounter.domain.profile.ProfileService;
import com.github.anyuoyuna.caloriecounter.dto.AssistantIntent;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.handler.ActivityMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.FinanceMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.FoodMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.QuestionMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

import static com.github.anyuoyuna.caloriecounter.bot.MenuKeyboard.*;

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

    public BotResponse route(User user, String text) {

        if (text.equals("/start")) {
            return BotResponse.plainWithMenu("С возвращением! Просто пиши мне, что съела, или как потренировалась.");
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
            return BotResponse.plain("Просто напиши, что ты съела (например: омлет из 2 яиц и кофе):");
        }
        if (text.equals(BTN_ACTIVITY)) {
            return BotResponse.plain("Опиши свою активность (например: бегала 30 минут):");
        }
        if (text.equals(BTN_QUESTION)) {
            return BotResponse.plain("Задай любой вопрос о питании или здоровье:");
        }
        if (text.equals("/import_history")) {
            int count = financeService.importHistoryFromSheets();
            return BotResponse.plain("Импорт завершен! Загружено строк: " + count);
        }

        log.info("Анализирую текст через AssistantService: {}", text);
        AssistantIntent intent = assistantService.analyze(text);

        return switch (intent.primaryIntent()) {
            case "FOOD" -> foodMessageHandler.handle(user, text);
            case "ACTIVITY" -> activityMessageHandler.handle(user, text);
            case "FINANCE" -> financeMessageHandler.handle(user, text);
            case "QUESTION" -> questionMessageHandler.handle(text);
            case "GREETING" -> BotResponse.plainWithMenu("Привет! Я на связи. Что сегодня запишем?");
            default -> {
                log.warn("Непонятный интент '{}', используем FoodHandler по умолчанию", intent.primaryIntent());
                yield foodMessageHandler.handle(user, text);
            }
        };
    }
}