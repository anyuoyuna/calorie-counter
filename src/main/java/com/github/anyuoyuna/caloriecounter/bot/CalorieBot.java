package com.github.anyuoyuna.caloriecounter.bot;

import com.github.anyuoyuna.caloriecounter.entity.FoodItem;
import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.repository.FoodItemRepository;
import com.github.anyuoyuna.caloriecounter.repository.MealEntryRepository;
import com.github.anyuoyuna.caloriecounter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class CalorieBot extends TelegramLongPollingBot {

    private final UserRepository userRepo;
    private final FoodItemRepository foodItemRepo;
    private final MealEntryRepository mealEntryRepo;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public CalorieBot(@Value("${telegram.bot.token}") String botToken,
                      UserRepository userRepo,
                      FoodItemRepository foodItemRepo,
                      MealEntryRepository mealEntryRepo) {
        super(botToken);
        this.userRepo = userRepo;
        this.foodItemRepo = foodItemRepo;
        this.mealEntryRepo = mealEntryRepo;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Long telegramId = update.getMessage().getFrom().getId();
        String text = update.getMessage().getText().trim();
        Long chatId = update.getMessage().getChatId();

        User user = userRepo.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(telegramId);
                    return userRepo.save(u);
                });

        if (text.equals("/start")) {
            sendText(chatId, "Привет! Пиши еду в формате: название;калории;белки;жиры;углеводы;граммы\nНапример: яйцо;78;6.3;5.3;0.6;50");
            return;
        }

        if (text.equals("/today")) {
            sendText(chatId, buildDailySummary(user));
            return;
        }

        // формат: название;калории;белки;жиры;углеводы;граммы
        String[] parts = text.split(";");
        if (parts.length != 6) {
            sendText(chatId, "Не поняла формат. Пример: яйцо;78;6.3;5.3;0.6;50");
            return;
        }

        try {
            String name = parts[0].trim();
            double calories = Double.parseDouble(parts[1].trim());
            double protein = Double.parseDouble(parts[2].trim());
            double fat = Double.parseDouble(parts[3].trim());
            double carbs = Double.parseDouble(parts[4].trim());
            double grams = Double.parseDouble(parts[5].trim());

            FoodItem foodItem = foodItemRepo.findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        FoodItem fi = new FoodItem();
                        fi.setName(name);
                        fi.setCalories(calories);
                        fi.setProtein(protein);
                        fi.setFat(fat);
                        fi.setCarbs(carbs);
                        return foodItemRepo.save(fi);
                    });

            MealEntry entry = new MealEntry();
            entry.setUser(user);
            entry.setFoodItem(foodItem);
            entry.setGrams(grams);
            mealEntryRepo.save(entry);

            sendText(chatId, "Записала: " + name + " (" + grams + " г)\n\n" + buildDailySummary(user));
        } catch (NumberFormatException e) {
            sendText(chatId, "Ошибка в числах. Проверь формат: название;калории;белки;жиры;углеводы;граммы");
        }
    }

    private String buildDailySummary(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<MealEntry> entries = mealEntryRepo.findByUserAndEatenAtBetween(user, startOfDay, endOfDay);

        double totalCalories = 0, totalProtein = 0, totalFat = 0, totalCarbs = 0;
        for (MealEntry e : entries) {
            double ratio = e.getGrams() / 100.0;
            totalCalories += e.getFoodItem().getCalories() * ratio;
            totalProtein += e.getFoodItem().getProtein() * ratio;
            totalFat += e.getFoodItem().getFat() * ratio;
            totalCarbs += e.getFoodItem().getCarbs() * ratio;
        }

        int remaining = user.getDailyCalorieGoal() - (int) totalCalories;

        return String.format("Сегодня: %.0f ккал (Б: %.1f, Ж: %.1f, У: %.1f)\nОсталось до цели: %d ккал",
                totalCalories, totalProtein, totalFat, totalCarbs, remaining);
    }

    private void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
