package com.github.anyuoyuna.lifeassistant.domain.food;

import com.github.anyuoyuna.lifeassistant.dto.ParsedFoodItem;
import com.github.anyuoyuna.lifeassistant.dto.ParsedMealResponse;
import com.github.anyuoyuna.lifeassistant.entity.FoodItem;
import com.github.anyuoyuna.lifeassistant.entity.MealEntry;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.enums.FoodSource;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.EmbeddingClient;
import com.github.anyuoyuna.lifeassistant.repository.FoodItemRepository;
import com.github.anyuoyuna.lifeassistant.repository.MealEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealRecordingService {

    private final FoodItemRepository foodItemRepo;
    private final MealEntryRepository mealEntryRepo;
    private final EmbeddingClient embeddingClient;
    private final MealInputValidator mealInputValidator;
    private final Clock clock;

    public record RecordingResult(List<MealEntry> savedEntries,
                                  List<String> unrecognizedNames,
                                  List<String> rejectedNames) {}

    @Transactional
    public RecordingResult recordMeal(User user, ParsedMealResponse parsed) {
        LocalDate date = parseDateOrToday(parsed.getDate());
        LocalDateTime eatenAt = LocalDateTime.of(date, LocalTime.now(clock));
        MealInputValidator.ValidationResult validationResult = mealInputValidator.validate(parsed);

        List<MealEntry> savedEntries = new ArrayList<>();
        List<String> unrecognizedNames = new ArrayList<>();

        for (ParsedFoodItem item : validationResult.acceptedItems()) {
            if (!item.isRecognized()) {
                log.warn("Продукт не распознан моделью: {}", item.getOriginalInput());
                unrecognizedNames.add(item.getOriginalInput());
                continue;
            }
            FoodItem foodItem = findOrCreateFoodItem(item);
            MealEntry entry = new MealEntry();
            entry.setUser(user);
            entry.setFoodItem(foodItem);
            entry.setFoodName(item.getOriginalInput());
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
        Optional<FoodItem> exactMatch = foodItemRepo.findByNameIgnoreCase(item.getCleanName());
        if (exactMatch.isPresent()) {
            log.info("Найден точный матч для '{}'", item.getCleanName());
            return exactMatch.get();
        }

        float[] newVector = embeddingClient.embed(item.getCleanName());
        if (newVector != null) {
            String vectorStr = toVectorString(newVector);
            Optional<FoodItem> similar = foodItemRepo.findMostSimilar(vectorStr);

            if (similar.isPresent() && similar.get().getEmbedding() != null) {
                double distance = cosineDistance(newVector, similar.get().getEmbedding());
                if (distance < 0.15) {
                    log.info("Семантический поиск: '{}' похоже на '{}' (dist={})",
                            item.getCleanName(), similar.get().getName(), String.format("%.4f", distance));
                    return similar.get();
                }
            }
        }

        log.info("Ничего не нашли для '{}', создаем новую запись", item.getCleanName());
        FoodItem fi = new FoodItem();
        fi.setName(item.getCleanName());
        fi.setSource(FoodSource.AI_GENERATED);
        fi.setEmbedding(newVector);

        double portionGrams = (item.getGrams() != null && item.getGrams() > 0) ? item.getGrams() : 100.0;

        double totalCal = (item.getTotalCalories() != null) ? item.getTotalCalories() : 0.0;
        double totalProt = (item.getTotalProtein() != null) ? item.getTotalProtein() : 0.0;
        double totalFat = (item.getTotalFat() != null) ? item.getTotalFat() : 0.0;
        double totalCarb = (item.getTotalCarbs() != null) ? item.getTotalCarbs() : 0.0;
        double totalFib = (item.getTotalFiber() != null) ? item.getTotalFiber() : 0.0;

        fi.setName(item.getCleanName());
        fi.setCalories((totalCal / portionGrams) * 100);
        fi.setProtein((totalProt / portionGrams) * 100);
        fi.setFat((totalFat / portionGrams) * 100);
        fi.setCarbs((totalCarb / portionGrams) * 100);
        fi.setFiber((totalFib / portionGrams) * 100);

        return foodItemRepo.save(fi);
    }

    private LocalDate parseDateOrToday(String dateStr) {
        try {
            return dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now(clock);
        } catch (Exception e) {
            log.warn("Не удалось распарсить дату '{}' от LLM, использую сегодня", dateStr);
            return LocalDate.now(clock);
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
