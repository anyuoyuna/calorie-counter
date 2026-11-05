package com.github.anyuoyuna.lifeassistant.domain.assistant;

import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.infrastructure.google.GoogleSheetsService;
import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UndoService {
    private final ExpenseRepository expenseRepo;
    private final GoogleSheetsService googleSheetsService;

    @Transactional
    public String undoLastAction(User user) {
        return expenseRepo.findFirstByUserOrderByCreatedAtDesc(user)
                .map(expense -> deleteExpense(expense))
                .orElse("Nothing to cancel.");
    }

    private String deleteExpense(Expense expense) {
        if (expense.getExternalId() != null) {
            googleSheetsService.deleteRowByUuid("Operations", expense.getExternalId());
        }
        expenseRepo.delete(expense);
        log.info("Cancelled expense {} for user {}", expense.getId(), expense.getUser().getTelegramId());
        return "Expense deleted: " + expense.getAmount() + " thb";
    }
}
