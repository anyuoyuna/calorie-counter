package com.github.anyuoyuna.caloriecounter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "food_items")
@Getter
@Setter
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double calories; // на 100г
    private Double protein;
    private Double fat;
    private Double carbs;
}
