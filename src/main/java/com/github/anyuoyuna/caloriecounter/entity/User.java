package com.github.anyuoyuna.caloriecounter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long telegramId;

    private String timezone = "Asia/Bangkok";
    private Integer dailyCalorieGoal = 2000;
    private Double dailyProteinGoal;
    private Double dailyFatGoal;
    private Double dailyCarbsGoal;
    private Double dailyFiberGoal;
}
