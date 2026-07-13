package com.github.anyuoyuna.caloriecounter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "weight_logs")
@Getter
@Setter
public class WeightLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDate loggedAt;
    private Double weightKg;
    private Double bodyFatPercent;
    private Double muscleWeight;
}
