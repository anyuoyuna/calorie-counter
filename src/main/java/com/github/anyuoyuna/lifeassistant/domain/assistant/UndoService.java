package com.github.anyuoyuna.lifeassistant.domain.assistant;

import com.github.anyuoyuna.lifeassistant.entity.*;
import com.github.anyuoyuna.lifeassistant.infrastructure.google.GoogleSheetsService;
import com.github.anyuoyuna.lifeassistant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class UndoService {

    private final MealEntryRepository mealEntryRepo;
    private final ActivityLogRepository activityLogRepo;
    private final ExpenseRepository expenseRepo;
    private final GoogleSheetsService googleSheetsService;

    @Transactional
    public String undoLastAction(User user) {
        Optional<MealEntry> lastMeal = mealEntryRepo.findFirstByUserOrderByCreatedAtDesc(user);
        Optional<ActivityLog> lastActivity = activityLogRepo.findFirstByUserOrderByCreatedAtDesc(user);
        Optional<Expense> lastExpense = expenseRepo.findFirstByUserOrderByCreatedAtDesc(user);
        record UndoCandidate(LocalDateTime time, Object entity, String type) {}
        Optional<UndoCandidate> winner = Stream.of(
                        lastMeal.map(m -> new UndoCandidate(m.getCreatedAt(), m, "MEAL")).orElse(null),
                        lastActivity.map(a -> new UndoCandidate(a.getCreatedAt(), a, "ACTIVITY")).orElse(null),
                        lastExpense.map(e -> new UndoCandidate(e.getCreatedAt(), e, "EXPENSE")).orElse(null)
                )
                .filter(Objects::nonNull)
                .filter(c -> c.time() != null)
                .max(Comparator.comparing(UndoCandidate::time));
        if (winner.isEmpty()) {
            return "Nothing to cancel (no timestamped entries found).";
        }
        UndoCandidate toUndo = winner.get();
        log.info("CANCEL: Type={}, Time={}, Entity={}", toUndo.type, toUndo.time, toUndo.entity);
        return switch (toUndo.type) {
            case "MEAL" -> {
                MealEntry meal = (MealEntry) toUndo.entity;
                String info = meal.getFoodName();
                mealEntryRepo.delete(meal);
                yield "✅ Food entry deleted: " + info;
            }
            case "ACTIVITY" -> {
                ActivityLog activity = (ActivityLog) toUndo.entity;
                String info = activity.getActivityType();
                activityLogRepo.delete(activity);
                yield "✅ Activity deleted: " + info;
            }
            case "EXPENSE" -> {
                Expense expense = (Expense) toUndo.entity;
                String info = expense.getAmount() + " thb";
                if (expense.getExternalId() != null) {
                    googleSheetsService.deleteRowByUuid("Operations", expense.getExternalId());
                }
                expenseRepo.delete(expense);
                yield "✅ Expense deleted: " + info;
            }
            default -> "Error.";
        };
    }
}