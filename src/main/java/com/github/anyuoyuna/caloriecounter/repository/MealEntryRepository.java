package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.MealEntry;
import com.github.anyuoyuna.caloriecounter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {
    List<MealEntry> findByUserAndEatenAtBetween(User user, LocalDateTime from, LocalDateTime to);
}
