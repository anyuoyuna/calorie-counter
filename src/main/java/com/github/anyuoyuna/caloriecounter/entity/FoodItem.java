package com.github.anyuoyuna.caloriecounter.entity;

import com.github.anyuoyuna.caloriecounter.entity.enums.FoodSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "food_items")
@Getter @Setter
public class FoodItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carbs;
    private Double fiber;

    @Enumerated(EnumType.STRING)
    private FoodSource source = FoodSource.LOCAL;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embedding;
}
