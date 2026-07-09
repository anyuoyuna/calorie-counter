package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
}
