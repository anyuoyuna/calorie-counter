package com.github.anyuoyuna.caloriecounter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_entries")
@Getter
@Setter
public class MealEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private FoodItem foodItem;

    private Double grams;
    private LocalDateTime eatenAt = LocalDateTime.now();
}
