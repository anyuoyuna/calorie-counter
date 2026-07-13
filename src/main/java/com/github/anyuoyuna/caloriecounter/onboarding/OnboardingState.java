package com.github.anyuoyuna.caloriecounter.onboarding;

import com.github.anyuoyuna.caloriecounter.entity.enums.ActivityLevel;
import com.github.anyuoyuna.caloriecounter.entity.enums.Gender;
import com.github.anyuoyuna.caloriecounter.entity.enums.GoalType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OnboardingState {
    private OnboardingStep currentStep = OnboardingStep.ASK_GENDER;

    private Gender gender;
    private LocalDate birthDate;
    private Double heightCm;
    private Double weightKg;
    private Double bodyFatPercent;
    private Double muscleWeight;
    private GoalType goalType;
    private ActivityLevel activityLevel;
    private Double targetWeightKg;
}
