package com.github.anyuoyuna.lifeassistant.domain.finance;

import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.enums.ExpenseCategory;
import com.github.anyuoyuna.lifeassistant.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceToolsTest {

    private static final LocalDate MONTH_START = LocalDate.of(2026, 11, 1);
    private static final LocalDate NEXT_MONTH_START = LocalDate.of(2026, 12, 1);

    private final ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"), ZoneOffset.UTC);
    private final FinanceTools financeTools = new FinanceTools(expenseRepository, clock);

    @Test
    void groupsAllUsersExpensesSortsCategoriesAndCalculatesPercentages() {
        User firstUser = new User();
        User secondUser = new User();
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of(
                expense(firstUser, ExpenseCategory.Transport, "25.00"),
                expense(secondUser, ExpenseCategory.Groceries, "75.00")
        ));

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo("""
                Total expenses this month: 100.00 baht.
                - Groceries: 75.00 baht (75.00%)
                - Transport: 25.00 baht (25.00%)""".stripTrailing());
        verify(expenseRepository).findExpensesByDateRange(MONTH_START, NEXT_MONTH_START);
        verify(expenseRepository, never()).findAll();
    }

    @Test
    void sumsExpensesInTheSameCategory() {
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of(
                expense(new User(), ExpenseCategory.Groceries, "10.25"),
                expense(new User(), ExpenseCategory.Groceries, "14.75")
        ));

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo("""
                Total expenses this month: 25.00 baht.
                - Groceries: 25.00 baht (100.00%)""".stripTrailing());
    }

    @Test
    void returnsNoExpensesMessageWhenRepositoryFindsNoCurrentMonthExpenses() {
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of());

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo("No expenses recorded for the current month.");
    }

    @Test
    void avoidsDivisionByZeroWhenCurrentMonthExpensesTotalZero() {
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of(
                expense(new User(), ExpenseCategory.Groceries, "0.00")
        ));

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo(
                "Total expenses this month: 0.00 baht.\n- Groceries: 0.00 baht");
    }

    @Test
    void roundsPercentagesToTwoDecimalPlaces() {
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of(
                expense(new User(), ExpenseCategory.Transport, "1.00"),
                expense(new User(), ExpenseCategory.Groceries, "2.00")
        ));

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo("""
                Total expenses this month: 3.00 baht.
                - Groceries: 2.00 baht (66.67%)
                - Transport: 1.00 baht (33.33%)""".stripTrailing());
    }

    @Test
    void sortsEqualCategoryTotalsByCategoryName() {
        when(expenseRepository.findExpensesByDateRange(MONTH_START, NEXT_MONTH_START)).thenReturn(List.of(
                expense(new User(), ExpenseCategory.Transport, "10.00"),
                expense(new User(), ExpenseCategory.Groceries, "10.00")
        ));

        String result = financeTools.getCurrentMonthExpenseBreakdown();

        assertThat(result).isEqualTo("""
                Total expenses this month: 20.00 baht.
                - Groceries: 10.00 baht (50.00%)
                - Transport: 10.00 baht (50.00%)""".stripTrailing());
    }

    private Expense expense(User user, ExpenseCategory category, String amount) {
        Expense expense = new Expense();
        expense.setUser(user);
        expense.setCategory(category);
        expense.setAmount(new BigDecimal(amount));
        expense.setType("-");
        return expense;
    }
}
