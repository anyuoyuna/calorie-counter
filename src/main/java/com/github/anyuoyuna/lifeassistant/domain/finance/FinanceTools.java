package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceTools {

    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    @Tool("Returns the total amount spent for a given category in the current month.")
    public String getMonthlySpending(String category) {
        String normalizedCategory = normalizeCategory(category);
        log.info("AI calling tool: getMonthlySpending. Category: {} (normalized to {})", category, normalizedCategory);
        LocalDate startOfMonth = LocalDate.now(clock).withDayOfMonth(1);
        BigDecimal total = expenseRepository.findAll().stream()
                .filter(e -> e.getCategory().name().equalsIgnoreCase(normalizedCategory))
                .filter(e -> !e.getDate().isBefore(startOfMonth))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Total spending for " + normalizedCategory + " this month is " + total + " baht.";
    }

    private String normalizeCategory(String category) {
        return switch (category.toLowerCase()) {
            case "транспорт", "transport" -> "Transport";
            case "еда", "продукты", "groceries" -> "Groceries";
            case "кафе", "ресторан", "eateries" -> "Eateries";
            case "здоровье", "health" -> "Health";
            case "доставка", "delivery" -> "Delivery";
            case "счета", "подсписки", "utilities" -> "Utilities";
            default -> category;
        };
    }
}