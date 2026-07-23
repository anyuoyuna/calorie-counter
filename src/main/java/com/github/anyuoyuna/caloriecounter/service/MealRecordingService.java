package com.github.anyuoyuna.caloriecounter.service;

import com.github.anyuoyuna.caloriecounter.dto.ParsedFoodItem;
import com.github.anyuoyuna.caloriecounter.dto.ParsedMealResponse;
import com.github.anyuoyuna.caloriecounter.entity.FoodItem;
import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.entity.enums.FoodSource;
import com.github.anyuoyuna.caloriecounter.repository.FoodItemRepository;
import com.github.anyuoyuna.caloriecounter.repository.MealEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MealRecordingService {

    private final FoodItemRepository foodItemRepo;
    private final MealEntryRepository mealEntryRepo;
    private final EmbeddingClient embeddingClient;
    private final MealInputValidator mealInputValidator;

    public MealRecordingService(FoodItemRepository foodItemRepo,
                                MealEntryRepository mealEntryRepo,
                                EmbeddingClient embeddingClient,
                                MealInputValidator mealInputValidator) {
        this.foodItemRepo = foodItemRepo;
        this.mealEntryRepo = mealEntryRepo;
        this.embeddingClient = embeddingClient;
        this.mealInputValidator = mealInputValidator;
    }

    public record RecordingResult(List<MealEntry> savedEntries,
                                  List<String> unrecognizedNames,
                                  List<String> rejectedNames) {}

    @Transactional
    public RecordingResult recordMeal(User user, ParsedMealResponse parsed) {
        LocalDate date = parseDateOrToday(parsed.getDate());
        LocalDateTime eatenAt = LocalDateTime.of(date, LocalTime.now());
        MealInputValidator.ValidationResult validationResult = mealInputValidator.validate(parsed);

        List<MealEntry> savedEntries = new ArrayList<>();
        List<String> unrecognizedNames = new ArrayList<>();

        for (ParsedFoodItem item : validationResult.acceptedItems()) {
            if (!item.isRecognized()) {
                log.warn("Продукт не распознан моделью: {}", item.getName());
                unrecognizedNames.add(item.getName());
                continue;
            }

            FoodItem foodItem = findOrCreateFoodItem(item);

            MealEntry entry = new MealEntry();
            entry.setUser(user);
            entry.setFoodItem(foodItem);
            entry.setGrams(item.getGrams());
            entry.setEatenAt(eatenAt);
            mealEntryRepo.save(entry);

            savedEntries.add(entry);
        }

        if (parsed.getNotes() != null && !parsed.getNotes().isEmpty()) {
            log.info("Заметки от LLM для пользователя {}: {}", user.getTelegramId(), parsed.getNotes());
        }

        log.info("Записано {} позиций еды для пользователя {}, дата={}, приём={}, не распознано={}",
                savedEntries.size(), user.getTelegramId(), date, parsed.getMeal(), unrecognizedNames.size());

        return new RecordingResult(savedEntries, unrecognizedNames, validationResult.rejectedItemNames());
    }

    private FoodItem findOrCreateFoodItem(ParsedFoodItem item) {
        float[] newEmbedding = embeddingClient.embed(item.getName());

        if (newEmbedding != null) {
            String vectorStr = toVectorString(newEmbedding);
            Optional<FoodItem> similar = foodItemRepo.findMostSimilar(vectorStr);

            if (similar.isPresent() && similar.get().getEmbedding() != null) {
                double distance = cosineDistance(newEmbedding, similar.get().getEmbedding());
                if (distance < 0.15) {
                    log.info("Найден похожий продукт '{}' для запроса '{}', расстояние={}",
                            similar.get().getName(), item.getName(), distance);
                    return similar.get();
                }
            }
        }

        FoodItem fi = new FoodItem();
        fi.setName(item.getName());
        fi.setCalories(item.getCalories());
        fi.setProtein(item.getProtein());
        fi.setFat(item.getFat());
        fi.setCarbs(item.getCarbs());
        fi.setFiber(item.getFiber());
        fi.setSource(FoodSource.AI_GENERATED);
        if (newEmbedding != null) {
            fi.setEmbedding(newEmbedding);
        }
        return foodItemRepo.save(fi);
    }

    private LocalDate parseDateOrToday(String dateStr) {
        try {
            return dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату '{}' от LLM, использую сегодня", dateStr);
            return LocalDate.now();
        }
    }

    private String toVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        return sb.append("]").toString();
    }

    private double cosineDistance(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return 1 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
