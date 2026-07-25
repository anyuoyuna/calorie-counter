package com.github.anyuoyuna.caloriecounter.bot;

import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.handler.ActivityMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.FoodMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.QuestionMessageHandler;
import com.github.anyuoyuna.caloriecounter.onboarding.OnboardingHandler;
import com.github.anyuoyuna.caloriecounter.domain.profile.ProfileEditHandler;
import com.github.anyuoyuna.caloriecounter.repository.UserRepository;
import com.github.anyuoyuna.caloriecounter.domain.food.DailyReportService;
import com.github.anyuoyuna.caloriecounter.domain.profile.ProfileService;
import com.github.anyuoyuna.caloriecounter.domain.food.WeeklySummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.anyuoyuna.caloriecounter.bot.MenuKeyboard.*;

@Slf4j
@Component
public class CalorieBot extends TelegramLongPollingBot {

    private final UserRepository userRepo;
    private final OnboardingHandler onboardingHandler;
    private final ProfileEditHandler profileEditHandler;
    private final ProfileService profileService;
    private final WeeklySummaryService weeklySummaryService;
    private final DailyReportService dailyReportService;
    private final FoodMessageHandler foodMessageHandler;
    private final ActivityMessageHandler activityMessageHandler;
    private final QuestionMessageHandler questionMessageHandler;
    private final MenuKeyboard menuKeyboard;

    private enum PendingAction { ACTIVITY, QUESTION }
    private final Map<Long, PendingAction> pendingActions = new ConcurrentHashMap<>();

    @Value("${telegram.bot.username}")
    private String botUsername;

    public CalorieBot(@Value("${telegram.bot.token}") String botToken,
                      UserRepository userRepo,
                      OnboardingHandler onboardingHandler,
                      ProfileEditHandler profileEditHandler,
                      ProfileService profileService,
                      WeeklySummaryService weeklySummaryService,
                      DailyReportService dailyReportService,
                      FoodMessageHandler foodMessageHandler,
                      ActivityMessageHandler activityMessageHandler,
                      QuestionMessageHandler questionMessageHandler,
                      MenuKeyboard menuKeyboard) {
        super(botToken);
        this.userRepo = userRepo;
        this.onboardingHandler = onboardingHandler;
        this.profileEditHandler = profileEditHandler;
        this.profileService = profileService;
        this.weeklySummaryService = weeklySummaryService;
        this.dailyReportService = dailyReportService;
        this.foodMessageHandler = foodMessageHandler;
        this.activityMessageHandler = activityMessageHandler;
        this.questionMessageHandler = questionMessageHandler;
        this.menuKeyboard = menuKeyboard;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasCallbackQuery()) {
            Long telegramId = update.getCallbackQuery().getFrom().getId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackData = update.getCallbackQuery().getData();

            if (onboardingHandler.isInProgress(telegramId)) {
                send(chatId, onboardingHandler.handleAnswer(telegramId, chatId, null, callbackData));
                if (!onboardingHandler.isInProgress(telegramId)) {
                    send(chatId, BotResponse.plainWithMenu("Теперь можешь пользоваться ботом 👇"));
                }
                return;
            }
            if (profileEditHandler.isInProgress(telegramId) || callbackData.startsWith("EDIT_MENU_")
                    || callbackData.startsWith("GOAL_") || callbackData.startsWith("ACT_")) {
                send(chatId, profileEditHandler.handle(telegramId, chatId, null, callbackData));
                return;
            }
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        Optional<User> existingUser = userRepo.findByTelegramId(telegramId);
        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setTelegramId(telegramId);
            userRepo.save(newUser);
            log.info("Зарегистрирован новый пользователь, telegramId={}", telegramId);

            onboardingHandler.startOnboarding(telegramId);
            send(chatId, onboardingHandler.firstQuestion(chatId));
            return;
        }

        User user = existingUser.get();

        if (onboardingHandler.isInProgress(telegramId)) {
            send(chatId, onboardingHandler.handleAnswer(telegramId, chatId, text, null));
            if (!onboardingHandler.isInProgress(telegramId)) {
                send(chatId, BotResponse.plainWithMenu("Теперь можешь пользоваться ботом 👇"));
            }
            return;
        }

        if (profileEditHandler.isInProgress(telegramId)) {
            send(chatId, profileEditHandler.handle(telegramId, chatId, text, null));
            return;
        }

        if (text.equals(BTN_TODAY) || text.equals("/today")) {
            send(chatId, BotResponse.htmlWithMenu(dailyReportService.buildDailySummary(user, LocalDate.now())));
            return;
        }
        if (text.equals(BTN_PROFILE) || text.equals("/profile")) {
            send(chatId, BotResponse.plain(profileService.buildProfileSummary(user)));
            send(chatId, profileEditHandler.editMenu(chatId));
            return;
        }
        if (text.equals(BTN_WEEK) || text.equals("/week")) {
            send(chatId, BotResponse.plain("Считаю неделю, секунду..."));
            send(chatId, BotResponse.plainWithMenu(weeklySummaryService.buildWeeklySummary(user)));
            return;
        }
        if (text.equals(BTN_ACTIVITY)) {
            pendingActions.put(telegramId, PendingAction.ACTIVITY);
            send(chatId, BotResponse.plain("Опиши тренировку:"));
            return;
        }
        if (text.equals(BTN_QUESTION)) {
            pendingActions.put(telegramId, PendingAction.QUESTION);
            send(chatId, BotResponse.plain("Задай вопрос:"));
            return;
        }
        if (text.equals(BTN_FOOD)) {
            send(chatId, BotResponse.plain("Просто напиши, что съела:"));
            return;
        }
        if (text.equals("/start")) {
            send(chatId, BotResponse.plainWithMenu("С возвращением! Просто пиши, что съела — я всё посчитаю."));
            return;
        }

        PendingAction pending = pendingActions.remove(telegramId);
        if (pending == PendingAction.ACTIVITY) {
            send(chatId, activityMessageHandler.handle(user, text));
            return;
        }
        if (pending == PendingAction.QUESTION) {
            send(chatId, questionMessageHandler.handle(text));
            return;
        }

        send(chatId, foodMessageHandler.handle(user, text));
    }

    private void send(Long chatId, BotResponse response) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response.text());
        if (response.html()) message.setParseMode("HTML");
        if (response.showMenu()) message.setReplyMarkup(menuKeyboard.build());
        send(chatId, message);
    }

    private void send(Long chatId, SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение в Telegram, chatId={}", chatId, e);
        }
    }
}