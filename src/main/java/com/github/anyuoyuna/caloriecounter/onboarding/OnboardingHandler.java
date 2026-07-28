package com.github.anyuoyuna.caloriecounter.onboarding;

import com.github.anyuoyuna.caloriecounter.domain.profile.OnboardingCompletionService;
import com.github.anyuoyuna.caloriecounter.entity.enums.ActivityLevel;
import com.github.anyuoyuna.caloriecounter.entity.enums.Gender;
import com.github.anyuoyuna.caloriecounter.entity.enums.GoalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OnboardingHandler {

    private final Map<Long, OnboardingState> activeSessions = new ConcurrentHashMap<>();

    private final OnboardingCompletionService completionService;

    public OnboardingHandler(OnboardingCompletionService completionService) {
        this.completionService = completionService;
    }

    public boolean isInProgress(Long telegramId) {
        return activeSessions.containsKey(telegramId);
    }

    public void startOnboarding(Long telegramId) {
        activeSessions.put(telegramId, new OnboardingState());
        log.info("Начат онбординг для пользователя {}", telegramId);
    }

    public SendMessage firstQuestion(Long chatId) {
        return simple(chatId, "Привет! Давай познакомимся. Как мне тебя называть?");
    }

    public SendMessage handleAnswer(Long telegramId, Long chatId, String text, String callbackData) {
        OnboardingState state = activeSessions.get(telegramId);
        if (state == null) {
            startOnboarding(telegramId);
            return firstQuestion(chatId);
        }

        String value = callbackData != null ? callbackData : text;

        switch (state.getCurrentStep()) {
            case ASK_NAME -> {
                if (value == null || value.isBlank()) {
                    return retry(chatId, "Пожалуйста, введи свое имя");
                }
                state.setDisplayName(value);
                state.setCurrentStep(OnboardingStep.ASK_GENDER);
                return askGender(chatId);
            }
            case ASK_GENDER -> {
                state.setGender(Gender.valueOf(value));
                state.setCurrentStep(OnboardingStep.ASK_BIRTH_DATE);
                return askBirthDate(chatId);
            }
            case ASK_BIRTH_DATE -> {
                LocalDate date = parseDate(value);
                if (date == null) return retry(chatId, "Не поняла дату. Формат: ДД.ММ.ГГГГ, например 15.03.1995");
                state.setBirthDate(date);
                state.setCurrentStep(OnboardingStep.ASK_HEIGHT);
                return askHeight(chatId);
            }
            case ASK_HEIGHT -> {
                Double height = parseDoubleInRange(value, 100, 250);
                if (height == null) return retry(chatId, "Рост должен быть числом от 100 до 250 см");
                state.setHeightCm(height);
                state.setCurrentStep(OnboardingStep.ASK_WEIGHT);
                return askWeight(chatId);
            }
            case ASK_WEIGHT -> {
                Double weight = parseDoubleInRange(value, 30, 300);
                if (weight == null) return retry(chatId, "Вес должен быть числом от 30 до 300 кг");
                state.setWeightKg(weight);
                state.setCurrentStep(OnboardingStep.ASK_BODY_FAT);
                return askBodyFat(chatId);
            }
            case ASK_BODY_FAT -> {
                if ("UNKNOWN".equals(value)) {
                    state.setBodyFatPercent(null);
                } else {
                    Double fat = parseDoubleInRange(value, 3, 60);
                    if (fat == null) return retry(chatId, "Введи число от 3 до 60, или нажми \"Не знаю\"");
                    state.setBodyFatPercent(fat);
                }
                state.setCurrentStep(OnboardingStep.ASK_MUSCLE_PERCENT);
                return askMusclePercent(chatId);
            }
            case ASK_MUSCLE_PERCENT -> {
                if ("UNKNOWN".equals(value)) {
                    state.setMuscleWeight(null);
                } else {
                    Double muscle = parseDoubleInRange(value, 10, 70);
                    if (muscle == null) return retry(chatId, "Введи число от 10 до 70, или нажми \"Не знаю\"");
                    state.setMuscleWeight(muscle);
                }
                state.setCurrentStep(OnboardingStep.ASK_GOAL);
                return askGoal(chatId);
            }
            case ASK_GOAL -> {
                state.setGoalType(GoalType.valueOf(value));
                state.setCurrentStep(OnboardingStep.ASK_ACTIVITY_LEVEL);
                return askActivityLevel(chatId);
            }
            case ASK_ACTIVITY_LEVEL -> {
                state.setActivityLevel(ActivityLevel.valueOf(value));
                state.setCurrentStep(OnboardingStep.ASK_TARGET_WEIGHT);
                return askTargetWeight(chatId);
            }
            case ASK_TARGET_WEIGHT -> {
                if ("SKIP".equals(value)) {
                    state.setTargetWeightKg(null);
                } else {
                    Double target = parseDoubleInRange(value, 30, 300);
                    if (target == null) return retry(chatId, "Введи число от 30 до 300 кг, или нажми \"Пропустить\"");
                    state.setTargetWeightKg(target);
                }
                return finishOnboarding(telegramId, chatId, state);
            }
            default -> {
                log.warn("Онбординг в неожиданном состоянии для пользователя {}: {}", telegramId, state.getCurrentStep());
                return retry(chatId, "Что-то пошло не так, начни заново через /start");
            }
        }
    }

    private SendMessage finishOnboarding(Long telegramId, Long chatId, OnboardingState state) {
        int dailyGoal = completionService.completeOnboarding(telegramId, state);

        activeSessions.remove(telegramId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(String.format(
                "Профиль готов!\nТвоя дневная цель: %d ккал\n\nТеперь просто пиши мне, что съела — я всё посчитаю.",
                dailyGoal));
        return message;
    }

    private SendMessage askGender(Long chatId) {
        return withButtons(chatId, "Для начала — укажи пол (нужно для расчёта нормы калорий):",
                List.of(button("Женский", "FEMALE"), button("Мужской", "MALE")));
    }

    private SendMessage askBirthDate(Long chatId) {
        return simple(chatId, "Дата рождения? Формат: ДД.ММ.ГГГГ");
    }

    private SendMessage askHeight(Long chatId) {
        return simple(chatId, "Рост в сантиметрах?");
    }

    private SendMessage askWeight(Long chatId) {
        return simple(chatId, "Текущий вес в кг?");
    }

    private SendMessage askBodyFat(Long chatId) {
        return withButtons(chatId, "Процент жира (если знаешь)?",
                List.of(button("Не знаю", "UNKNOWN")));
    }

    private SendMessage askMusclePercent(Long chatId) {
        return withButtons(chatId, "Вес мышечной массы (если знаешь)?",
                List.of(button("Не знаю", "UNKNOWN")));
    }

    private SendMessage askGoal(Long chatId) {
        return withButtons(chatId, "Какая у тебя цель?",
                List.of(
                        button("Похудение, сохраняя мышцы", "LOSE_WEIGHT_KEEP_MUSCLE"),
                        button("Похудение", "LOSE_WEIGHT"),
                        button("Поддержание веса", "MAINTAIN"),
                        button("Набор массы", "GAIN_MUSCLE")
                ));
    }
//TODO исправить тут на textblock
    private SendMessage askActivityLevel(Long chatId) {
        return withButtons(chatId,
                "Базовый уровень активности (без учёта отдельных тренировок)?\n\n" +
                        "Сидячий — почти не двигаюсь\n" +
                        "Лёгкий — активность 1-3 раза в неделю\n" +
                        "Умеренный — 3-5 раз в неделю\n" +
                        "Высокий — 6-7 раз в неделю",
                List.of(
                        button("Сидячий", "SEDENTARY"),
                        button("Лёгкий", "LIGHT"),
                        button("Умеренный", "MODERATE"),
                        button("Высокий", "ACTIVE")
                ));
    }

    private SendMessage askTargetWeight(Long chatId) {
        return withButtons(chatId, "Желаемый вес в кг? (можно пропустить)",
                List.of(button("Пропустить", "SKIP")));
    }

    private SendMessage simple(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        return message;
    }

    private SendMessage retry(Long chatId, String errorText) {
        return simple(chatId, errorText);
    }

    private SendMessage withButtons(Long chatId, String text, List<InlineKeyboardButton> buttons) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        List<List<InlineKeyboardButton>> rows = buttons.stream()
                .map(List::of)
                .toList();

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);
        return message;
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(label);
        btn.setCallbackData(callbackData);
        return btn;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDoubleInRange(String value, double min, double max) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", "."));
            if (parsed < min || parsed > max) return null;
            return parsed;
        } catch (Exception e) {
            return null;
        }
    }
}