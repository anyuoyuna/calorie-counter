package com.github.anyuoyuna.caloriecounter.entity;

import com.github.anyuoyuna.caloriecounter.entity.enums.ActivityLevel;
import com.github.anyuoyuna.caloriecounter.entity.enums.Gender;
import com.github.anyuoyuna.caloriecounter.entity.enums.GoalType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
public class UserProfile {
    @Id
    private Long userId; // совпадает с user.id, это и есть 1:1 связь

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalDate birthDate;
    private Double heightCm;

    @Enumerated(EnumType.STRING)
    private GoalType goalType;

    @Enumerated(EnumType.STRING)
    private ActivityLevel activityLevel;

    private Double targetWeightKg;
}
