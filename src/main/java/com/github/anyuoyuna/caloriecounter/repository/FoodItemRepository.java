package com.github.anyuoyuna.caloriecounter.repository;

import com.github.anyuoyuna.caloriecounter.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    Optional<FoodItem> findByNameIgnoreCase(String name);
}
