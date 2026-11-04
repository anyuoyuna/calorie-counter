package com.github.anyuoyuna.lifeassistant.repository;

import com.github.anyuoyuna.lifeassistant.entity.Expense;
import com.github.anyuoyuna.lifeassistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    Optional<Expense> findFirstByUserOrderByCreatedAtDesc(User user);

    @Query("select e from Expense e where e.date >= :startDate and e.date < :endDate and e.type = '-'")
    List<Expense> findExpensesByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
