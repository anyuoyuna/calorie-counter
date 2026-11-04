package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Tool("Returns a current-month breakdown of all users expenses by category, including totals and percentages.")
    public String getCurrentMonthExpenseBreakdown() {
        LocalDate startOfMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);
        log.info("AI calling tool: getCurrentMonthExpenseBreakdown. Range: [{}; {})",
                startOfMonth, startOfNextMonth);

        Map<String, BigDecimal> totalsByCategory = expenseRepository
                .findExpensesByDateRange(startOfMonth, startOfNextMonth)
                .stream()
                .collect(Collectors.groupingBy(
                        expense -> expense.getCategory().name(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        if (totalsByCategory.isEmpty()) {
            return "No expenses recorded for the current month.";
        }

        BigDecimal total = totalsByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder result = new StringBuilder("Total expenses this month: ")
                .append(formatAmount(total))
                .append(" baht.");
        boolean hasZeroTotal = total.compareTo(BigDecimal.ZERO) == 0;
        totalsByCategory.entrySet().stream()
                .sorted((left, right) -> {
                    int amountComparison = right.getValue().compareTo(left.getValue());
                    return amountComparison != 0
                            ? amountComparison
                            : left.getKey().compareTo(right.getKey());
                })
                .forEach(entry -> {
                    result.append("\n- ")
                            .append(entry.getKey())
                            .append(": ")
                            .append(formatAmount(entry.getValue()))
                            .append(" baht");
                    if (!hasZeroTotal) {
                        BigDecimal percentage = entry.getValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP);
                        result.append(" (")
                                .append(percentage.toPlainString())
                                .append("%)");
                    }
                });
        return result.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
