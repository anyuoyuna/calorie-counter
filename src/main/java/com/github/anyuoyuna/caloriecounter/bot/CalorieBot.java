package com.github.anyuoyuna.caloriecounter.bot;

import com.github.anyuoyuna.caloriecounter.domain.assistant.AssistantService;
import com.github.anyuoyuna.caloriecounter.domain.food.DailyReportService;
import com.github.anyuoyuna.caloriecounter.domain.food.WeeklySummaryService;
import com.github.anyuoyuna.caloriecounter.domain.profile.ProfileEditHandler;
import com.github.anyuoyuna.caloriecounter.domain.profile.ProfileService;
import com.github.anyuoyuna.caloriecounter.dto.AssistantIntent;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.handler.ActivityMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.FoodMessageHandler;
import com.github.anyuoyuna.caloriecounter.handler.QuestionMessageHandler;
import com.github.anyuoyuna.caloriecounter.onboarding.OnboardingHandler;
import com.github.anyuoyuna.caloriecounter.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

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
    private final AssistantService assistantService;
    private final Clock clock;

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
                      MenuKeyboard menuKeyboard,
                      AssistantService assistantService,
                      Clock clock) {
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
        this.assistantService = assistantService;
        this.clock = clock;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. Обработка кнопок под сообщениями (Inline)
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }

        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        // 2. Проверка пользователя и Онбординг
        Optional<User> existingUser = userRepo.findByTelegramId(telegramId);
        if (existingUser.isEmpty()) {
            startNewUserFlow(telegramId, chatId);
            return;
        }

        User user = existingUser.get();

        if (onboardingHandler.isInProgress(telegramId)) {
            send(chatId, onboardingHandler.handleAnswer(telegramId, chatId, text, null));
            if (!onboardingHandler.isInProgress(telegramId)) {
                send(chatId, BotResponse.plainWithMenu("Профиль настроен! Теперь просто пиши мне, что съела 👇"));
            }
            return;
        }

        // 3. Редактирование профиля
        if (profileEditHandler.isInProgress(telegramId)) {
            send(chatId, profileEditHandler.handle(telegramId, chatId, text, null));
            return;
        }

        // 4. Системные команды и кнопки меню
        if (handleExplicitCommands(chatId, user, text)) {
            return;
        }

        // 5. ИИ-ДИСПЕТЧЕР (Центральный мозг ассистента)
        log.info("Определяю намерение пользователя для текста: '{}'", text);
        AssistantIntent intent = assistantService.analyze(text);

        dispatchIntent(chatId, user, text, intent);
    }

    /**
     * Вызывает нужный обработчик в зависимости от того, что ИИ увидел в тексте
     */
    private void dispatchIntent(Long chatId, User user, String text, AssistantIntent intent) {
        switch (intent.primaryIntent()) {
            case "FOOD" -> send(chatId, foodMessageHandler.handle(user, text));
            case "ACTIVITY" -> send(chatId, activityMessageHandler.handle(user, text));
            case "QUESTION" -> send(chatId, questionMessageHandler.handle(text));
            case "GREETING" -> send(chatId, BotResponse.plainWithMenu("Привет! Я на связи. Что сегодня запишем: еду или тренировку?"));
            default -> {
                log.warn("Непонятный интент '{}', по умолчанию используем FoodHandler", intent.primaryIntent());
                send(chatId, foodMessageHandler.handle(user, text));
            }
        }
    }

    /**
     * Обработка фиксированных команд и кнопок нижнего меню
     */
    private boolean handleExplicitCommands(Long chatId, User user, String text) {
        if (text.equals("/start")) {
            send(chatId, BotResponse.plainWithMenu("С возвращением! О чем хочешь рассказать?"));
            return true;
        }
        if (text.equals(BTN_TODAY) || text.equals("/today")) {
            send(chatId, BotResponse.htmlWithMenu(dailyReportService.buildDailySummary(user, LocalDate.now(clock))));
            return true;
        }
        if (text.equals(BTN_PROFILE) || text.equals("/profile")) {
            send(chatId, BotResponse.plain(profileService.buildProfileSummary(user)));
            send(chatId, profileEditHandler.editMenu(chatId));
            return true;
        }
        if (text.equals(BTN_WEEK) || text.equals("/week")) {
            send(chatId, BotResponse.plain("Считаю статистику за неделю..."));
            send(chatId, BotResponse.plainWithMenu(weeklySummaryService.buildWeeklySummary(user)));
            return true;
        }
        // Подсказки для пользователя при нажатии на кнопки записи
        if (text.equals(BTN_FOOD)) {
            send(chatId, BotResponse.plain("Просто опиши свою еду в свободном стиле."));
            return true;
        }
        if (text.equals(BTN_ACTIVITY)) {
            send(chatId, BotResponse.plain("Опиши свою активность или тренировку."));
            return true;
        }
        if (text.equals(BTN_QUESTION)) {
            send(chatId, BotResponse.plain("Задай любой вопрос о питании или здоровье."));
            return true;
        }
        return false;
    }

    private void handleCallback(Update update) {
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
        }
    }

    private void startNewUserFlow(Long telegramId, Long chatId) {
        User newUser = new User();
        newUser.setTelegramId(telegramId);
        userRepo.save(newUser);
        log.info("Зарегистрирован новый пользователь: {}", telegramId);

        onboardingHandler.startOnboarding(telegramId);
        send(chatId, onboardingHandler.firstQuestion(chatId));
    }

    private void send(Long chatId, BotResponse response) {
        if (response == null) return;
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(response.text());
        if (response.html()) message.setParseMode("HTML");
        if (response.showMenu()) message.setReplyMarkup(menuKeyboard.build());
        executeMessage(message);
    }

    private void send(Long chatId, SendMessage message) {
        if (message == null) return;
        // Убедимся, что chatId установлен
        message.setChatId(chatId.toString());
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения в Telegram", e);
        }
    }
}