package com.github.anyuoyuna.caloriecounter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "activity_logs")
@Getter
@Setter
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private LocalDate activityDate;
    private String activityType;
    private Integer durationMinutes;
    private Integer estimatedCaloriesBurned;
}
