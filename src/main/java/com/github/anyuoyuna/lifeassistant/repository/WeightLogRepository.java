package com.github.anyuoyuna.lifeassistant.repository;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeightLogRepository extends JpaRepository<WeightLog, Long> {
    List<WeightLog> findByUserOrderByLoggedAtDesc(User user);
    Optional<WeightLog> findFirstByUserOrderByLoggedAtAsc(User user);
    Optional<WeightLog> findFirstByUserOrderByLoggedAtDesc(User user);
}
