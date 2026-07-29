package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.Expense;
import com.github.anyuoyuna.caloriecounter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    Optional<Expense> findFirstByUserOrderByCreatedAtDesc(User user);
}
