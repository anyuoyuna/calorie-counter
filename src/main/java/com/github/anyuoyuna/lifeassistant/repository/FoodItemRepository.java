package com.github.anyuoyuna.lifeassistant.repository;

import com.github.anyuoyuna.lifeassistant.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    Optional<FoodItem> findByNameIgnoreCase(String name);

    @Query(value = """
            SELECT * FROM food_items
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT 1
            """, nativeQuery = true)
    Optional<FoodItem> findMostSimilar(@Param("queryVector") String queryVector);
}
