package com.github.anyuoyuna.lifeassistant.repository;

import com.github.anyuoyuna.lifeassistant.entity.MealEntry;
import com.github.anyuoyuna.lifeassistant.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MealEntryRepository extends JpaRepository<MealEntry, Long> {
    List<MealEntry> findByUserAndEatenAtBetween(User user, LocalDateTime from, LocalDateTime to);
    Optional<MealEntry> findFirstByUserOrderByCreatedAtDesc(User user);

}
