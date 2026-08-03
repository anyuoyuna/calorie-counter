package com.github.anyuoyuna.lifeassistant.domain.profile;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
import com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel;
import com.github.anyuoyuna.lifeassistant.entity.enums.GoalType;
import com.github.anyuoyuna.lifeassistant.repository.UserProfileRepository;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import com.github.anyuoyuna.lifeassistant.repository.WeightLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ProfileEditHandler {

    private final Map<Long, EditSession> activeSessions = new ConcurrentHashMap<>();

    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final WeightLogRepository weightLogRepo;
    private final CalorieCalculationService calorieService;

    public ProfileEditHandler(UserRepository userRepo,
                              UserProfileRepository profileRepo,
                              WeightLogRepository weightLogRepo,
                              CalorieCalculationService calorieService) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.weightLogRepo = weightLogRepo;
        this.calorieService = calorieService;
    }

    public boolean isInProgress(Long telegramId) {
        return activeSessions.containsKey(telegramId);
    }

    // Меню редактирования, показывается под /profile
    public SendMessage editMenu(Long chatId) {
        return withButtons(chatId, "Что хочешь изменить?", List.of(
                button("Вес / жир / мышцы", "EDIT_MENU_WEIGHT"),
                button("Цель", "EDIT_MENU_GOAL"),
                button("Активность", "EDIT_MENU_ACTIVITY"),
                button("Желаемый вес", "EDIT_MENU_TARGET_WEIGHT")
        ));
    }

    // Роутер для нажатий кнопок и текстовых ответов
    public SendMessage handle(Long telegramId, Long chatId, String text, String callbackData) {
        String value = callbackData != null ? callbackData : text;

        // Точки входа из меню
        if ("EDIT_MENU_WEIGHT".equals(value)) {
            activeSessions.put(telegramId, sessionWithStep(EditStep.EDIT_WEIGHT));
            return simple(chatId, "Текущий вес в кг?");
        }
        if ("EDIT_MENU_GOAL".equals(value)) {
            return withButtons(chatId, "Новая цель?", List.of(
                    button("Похудение, сохраняя мышцы", "GOAL_LOSE_WEIGHT_KEEP_MUSCLE"),
                    button("Похудение", "GOAL_LOSE_WEIGHT"),
                    button("Поддержание веса", "GOAL_MAINTAIN"),
                    button("Набор массы", "GOAL_GAIN_MUSCLE")
            ));
        }
        if ("EDIT_MENU_ACTIVITY".equals(value)) {
            return withButtons(chatId, "Новый уровень активности?", List.of(
                    button("Сидячий", "ACT_SEDENTARY"),
                    button("Лёгкий", "ACT_LIGHT"),
                    button("Умеренный", "ACT_MODERATE"),
                    button("Высокий", "ACT_ACTIVE")
            ));
        }
        if ("EDIT_MENU_TARGET_WEIGHT".equals(value)) {
            activeSessions.put(telegramId, sessionWithStep(EditStep.EDIT_TARGET_WEIGHT));
            return simple(chatId, "Желаемый вес в кг?");
        }

        // Однократные изменения (цель / активность) - применяются сразу, без сессии
        if (value.startsWith("GOAL_")) {
            GoalType goal = GoalType.valueOf(value.substring("GOAL_".length()));
            return applyGoalChange(telegramId, chatId, goal);
        }
        if (value.startsWith("ACT_")) {
            ActivityLevel activity = mapActivityCode(value);
            return applyActivityChange(telegramId, chatId, activity);
        }

        // Многошаговые флоу (вес -> жир -> мышцы)
        EditSession session = activeSessions.get(telegramId);
        if (session == null) return simple(chatId, "Нет активного редактирования, начни через /profile");

        return switch (session.getStep()) {
            case EDIT_WEIGHT -> {
                Double weight = parseDoubleInRange(value, 30, 300);
                if (weight == null) yield simple(chatId, "Вес должен быть числом от 30 до 300 кг");
                session.setPendingWeight(weight);
                session.setStep(EditStep.EDIT_BODY_FAT);
                yield withButtons(chatId, "Процент жира (если знаешь)?", List.of(button("Не знаю", "UNKNOWN")));
            }
            case EDIT_BODY_FAT -> {
                if (!"UNKNOWN".equals(value)) {
                    Double fat = parseDoubleInRange(value, 3, 60);
                    if (fat == null) yield simple(chatId, "Введи число от 3 до 60, или нажми \"Не знаю\"");
                    session.setPendingBodyFat(fat);
                }
                session.setStep(EditStep.EDIT_MUSCLE);
                yield withButtons(chatId, "Вес мышц (если знаешь)?", List.of(button("Не знаю", "UNKNOWN")));
            }
            case EDIT_MUSCLE -> {
                Double muscle = "UNKNOWN".equals(value) ? null : parseDoubleInRange(value, 10, 70);
                if (!"UNKNOWN".equals(value) && muscle == null)
                    yield simple(chatId, "Введи число от 10 до 70, или нажми \"Не знаю\"");
                yield applyWeightUpdate(telegramId, chatId, session.getPendingWeight(), session.getPendingBodyFat(), muscle);
            }
            case EDIT_TARGET_WEIGHT -> {
                Double target = parseDoubleInRange(value, 30, 300);
                if (target == null) yield simple(chatId, "Введи число от 30 до 300 кг");
                yield applyTargetWeightChange(telegramId, chatId, target);
            }
            case NONE -> simple(chatId, "Нет активного редактирования, начни через /profile");
        };
    }

    @Transactional
    protected SendMessage applyWeightUpdate(Long telegramId, Long chatId, Double weight, Double bodyFat, Double muscle) {
        User user = userRepo.findByTelegramId(telegramId).orElseThrow();
        UserProfile profile = profileRepo.findById(user.getId()).orElseThrow();

        WeightLog weightLog = new WeightLog(); // переименовано log -> weightLog
        weightLog.setUser(user);
        weightLog.setLoggedAt(LocalDate.now());
        weightLog.setWeightKg(weight);
        weightLog.setBodyFatPercent(bodyFat);
        weightLog.setMuscleWeight(muscle);
        weightLogRepo.save(weightLog);

        CalorieCalculationService.NutritionTargets targets = calorieService.calculateTargets(profile, weight);
        user.setDailyCalorieGoal(targets.calories());
        user.setDailyProteinGoal(targets.protein());
        user.setDailyFatGoal(targets.fat());
        user.setDailyCarbsGoal(targets.carbs());
        user.setDailyFiberGoal(targets.fiber());
        userRepo.save(user);

        activeSessions.remove(telegramId);
        log.info("Обновлён вес пользователя {}: {} кг, новая цель {} ккал", telegramId, weight, targets.calories()); // теперь log - это логгер

        return simple(chatId, "Обновила! Новая дневная цель: " + targets.calories() + " ккал");
    }

    @Transactional
    protected SendMessage applyGoalChange(Long telegramId, Long chatId, GoalType goal) {
        User user = userRepo.findByTelegramId(telegramId).orElseThrow();
        UserProfile profile = profileRepo.findById(user.getId()).orElseThrow();
        profile.setGoalType(goal);
        profileRepo.save(profile);

        Double latestWeight = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user)
                .map(WeightLog::getWeightKg).orElse(null);
        if (latestWeight != null) {
            CalorieCalculationService.NutritionTargets targets = calorieService.calculateTargets(profile, latestWeight);
            user.setDailyCalorieGoal(targets.calories());
            user.setDailyProteinGoal(targets.protein());
            user.setDailyFatGoal(targets.fat());
            user.setDailyCarbsGoal(targets.carbs());
            user.setDailyFiberGoal(targets.fiber());
            userRepo.save(user);
            return simple(chatId, "Цель обновлена! Новая дневная норма: " + targets.calories() + " ккал");
        }
        return simple(chatId, "Цель обновлена!");
    }

    @Transactional
    protected SendMessage applyActivityChange(Long telegramId, Long chatId, ActivityLevel activity) {
        User user = userRepo.findByTelegramId(telegramId).orElseThrow();
        UserProfile profile = profileRepo.findById(user.getId()).orElseThrow();
        profile.setActivityLevel(activity);
        profileRepo.save(profile);

        Double latestWeight = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user)
                .map(WeightLog::getWeightKg).orElse(null);
        if (latestWeight != null) {
            CalorieCalculationService.NutritionTargets targets = calorieService.calculateTargets(profile, latestWeight);
            user.setDailyCalorieGoal(targets.calories());
            user.setDailyProteinGoal(targets.protein());
            user.setDailyFatGoal(targets.fat());
            user.setDailyCarbsGoal(targets.carbs());
            user.setDailyFiberGoal(targets.fiber());
            userRepo.save(user);
            return simple(chatId, "Активность обновлена! Новая дневная норма: " + targets.calories() + " ккал");
        }
        return simple(chatId, "Активность обновлена!");
    }

    @Transactional
    protected SendMessage applyTargetWeightChange(Long telegramId, Long chatId, Double target) {
        User user = userRepo.findByTelegramId(telegramId).orElseThrow();
        UserProfile profile = profileRepo.findById(user.getId()).orElseThrow();
        profile.setTargetWeightKg(target);
        profileRepo.save(profile);

        activeSessions.remove(telegramId);
        return simple(chatId, "Желаемый вес обновлён: " + target + " кг");
    }

    private ActivityLevel mapActivityCode(String value) {
        return ActivityLevel.valueOf(value.substring("ACT_".length()));
    }

    private EditSession sessionWithStep(EditStep step) {
        EditSession s = new EditSession();
        s.setStep(step);
        return s;
    }

    private SendMessage simple(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        return message;
    }

    private SendMessage withButtons(Long chatId, String text, List<InlineKeyboardButton> buttons) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(buttons.stream().map(List::of).toList());
        message.setReplyMarkup(markup);
        return message;
    }

    private InlineKeyboardButton button(String label, String callbackData) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(label);
        btn.setCallbackData(callbackData);
        return btn;
    }

    private Double parseDoubleInRange(String value, double min, double max) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", "."));
            return (parsed < min || parsed > max) ? null : parsed;
        } catch (Exception e) {
            return null;
        }
    }
}
