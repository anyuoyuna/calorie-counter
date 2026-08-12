package com.github.anyuoyuna.lifeassistant.onboarding;

import com.github.anyuoyuna.lifeassistant.domain.profile.OnboardingCompletionService;
import com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel;
import com.github.anyuoyuna.lifeassistant.entity.enums.Gender;
import com.github.anyuoyuna.lifeassistant.entity.enums.GoalType;
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
        log.info("Onboarding started for user {}", telegramId);
    }

    public SendMessage firstQuestion(Long chatId) {
        return simple(chatId, "Hi! Let's get to know each other. What should I call you?");
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
                    return retry(chatId, "Please enter your name");
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
                if (date == null) return retry(chatId, "Didn't recognize the date. Format: DD.MM.YYYY, e.g., 15.03.1995");
                state.setBirthDate(date);
                state.setCurrentStep(OnboardingStep.ASK_HEIGHT);
                return askHeight(chatId);
            }
            case ASK_HEIGHT -> {
                Double height = parseDoubleInRange(value, 100, 250);
                if (height == null) return retry(chatId, "Height must be a number between 100 and 250 cm");
                state.setHeightCm(height);
                state.setCurrentStep(OnboardingStep.ASK_WEIGHT);
                return askWeight(chatId);
            }
            case ASK_WEIGHT -> {
                Double weight = parseDoubleInRange(value, 30, 300);
                if (weight == null) return retry(chatId, "Weight must be a number between 30 and 300 kg");
                state.setWeightKg(weight);
                state.setCurrentStep(OnboardingStep.ASK_BODY_FAT);
                return askBodyFat(chatId);
            }
            case ASK_BODY_FAT -> {
                if ("UNKNOWN".equals(value)) {
                    state.setBodyFatPercent(null);
                } else {
                    Double fat = parseDoubleInRange(value, 3, 60);
                    if (fat == null) return retry(chatId, "Enter a number between 3 and 60, or tap \"I don't know\"");
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
                    if (muscle == null) return retry(chatId, "Enter a number between 10 and 70, or tap \"I don't know\"");
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
                    if (target == null) return retry(chatId, "Enter a number between 30 and 300 kg, or tap \"Skip\"");
                    state.setTargetWeightKg(target);
                }
                return finishOnboarding(telegramId, chatId, state);
            }
            default -> {
                log.warn("Onboarding in unexpected state for user {}: {}", telegramId, state.getCurrentStep());
                return retry(chatId, "Something went wrong, please start over with /start");
            }
        }
    }

    private SendMessage finishOnboarding(Long telegramId, Long chatId, OnboardingState state) {
        int dailyGoal = completionService.completeOnboarding(telegramId, state);
        activeSessions.remove(telegramId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(String.format(
                "Profile is ready!\nnYour daily goal: %d kcal\n\nNow just tell me what you've eaten — I'll track everything.",
                dailyGoal));
        return message;
    }

    private SendMessage askGender(Long chatId) {
        return withButtons(chatId, "To start, select your sex (needed to calculate your calorie intake):",
                List.of(button("Female", "FEMALE"), button("Male", "MALE")));
    }

    private SendMessage askBirthDate(Long chatId) {
        return simple(chatId, "Date of birth? Format: DD.MM.YYYY");
    }

    private SendMessage askHeight(Long chatId) {
        return simple(chatId, "Height in centimeters?");
    }

    private SendMessage askWeight(Long chatId) {
        return simple(chatId, "Current weight in kg?");
    }

    private SendMessage askBodyFat(Long chatId) {
        return withButtons(chatId, "Body fat percentage (if you know it)?",
                List.of(button("I don't know", "UNKNOWN")));
    }

    private SendMessage askMusclePercent(Long chatId) {
        return withButtons(chatId, "Muscle mass (if you know it)?",
                List.of(button("I don't know", "UNKNOWN")));
    }

    private SendMessage askGoal(Long chatId) {
        return withButtons(chatId, "What is your goal?",
                List.of(
                        button("Lose weight keep muscle", "LOSE_WEIGHT_KEEP_MUSCLE"),
                        button("Lose weight", "LOSE_WEIGHT"),
                        button("Maintain", "MAINTAIN"),
                        button("Gain muscle", "GAIN_MUSCLE")
                ));
    }

    private SendMessage askActivityLevel(Long chatId) {
        return withButtons(chatId,
                """
                        Base activity level (excluding specific workouts)?
                        Sedentary — almost no movement
                        Light — active 1–3 times a week
                        Moderate — active 3–5 times a week
                        High — active 6–7 times a week
                     """,
                List.of(
                        button("sedentary", "SEDENTARY"),
                        button("Light", "LIGHT"),
                        button("Moderate", "MODERATE"),
                        button("High", "ACTIVE")
                ));
    }

    private SendMessage askTargetWeight(Long chatId) {
        return withButtons(chatId, "Target weight in kg? (optional)",
                List.of(button("Skip", "SKIP")));
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