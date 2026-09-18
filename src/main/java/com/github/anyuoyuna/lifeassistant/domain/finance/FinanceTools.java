package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceTools {

    private final ExpenseRepository expenseRepository;

    @Tool("Returns the total amount spent for a given category in the current month.")
    public String getMonthlySpending(String category) {
        log.info("AI is calling tool: getMonthlySpending for category: {}", category);
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        BigDecimal total = expenseRepository.findAll().stream()
                .filter(e -> e.getCategory().name().equalsIgnoreCase(category))
                .filter(e -> !e.getDate().isBefore(startOfMonth))
                .map(e -> e.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Total spending for " + category + " this month is " + total + " baht.";
    }
}