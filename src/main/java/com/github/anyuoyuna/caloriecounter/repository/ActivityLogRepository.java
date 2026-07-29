package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.ActivityLog;
import com.github.anyuoyuna.caloriecounter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserAndActivityDate(User user, LocalDate date);
    Optional<ActivityLog> findFirstByUserOrderByCreatedAtDesc(User user);
}
