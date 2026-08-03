package com.github.anyuoyuna.lifeassistant.onboarding;

import com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel;
import com.github.anyuoyuna.lifeassistant.entity.enums.Gender;
import com.github.anyuoyuna.lifeassistant.entity.enums.GoalType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OnboardingState {
    private OnboardingStep currentStep = OnboardingStep.ASK_NAME;
    private String displayName;
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
