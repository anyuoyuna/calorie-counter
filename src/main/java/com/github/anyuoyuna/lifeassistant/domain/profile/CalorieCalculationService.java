package com.github.anyuoyuna.lifeassistant.domain.profile;

import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel;
import com.github.anyuoyuna.lifeassistant.entity.enums.Gender;
import com.github.anyuoyuna.lifeassistant.entity.enums.GoalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
public class CalorieCalculationService {

    public record NutritionTargets(int calories, double protein, double fat, double carbs, double fiber) {}

    public NutritionTargets calculateTargets(UserProfile profile, double currentWeightKg) {
        double bmr = calculateBmr(profile, currentWeightKg);
        double tdee = calculateTdee(bmr, profile.getActivityLevel());
        double calorieGoal = applyGoalAdjustment(tdee, profile.getGoalType());

        double proteinPerKg = switch (profile.getGoalType()) {
            case LOSE_WEIGHT_KEEP_MUSCLE -> 2.0;
            case LOSE_WEIGHT -> 1.6;
            case MAINTAIN -> 1.6;
            case GAIN_MUSCLE -> 1.8;
        };
        double proteinGrams = proteinPerKg * currentWeightKg;
        double proteinCalories = proteinGrams * 4;

        double fatCalories = calorieGoal * 0.28;
        double fatGrams = fatCalories / 9;

        double carbsCalories = Math.max(calorieGoal - proteinCalories - fatCalories, 0);
        double carbsGrams = carbsCalories / 4;

        double fiberGrams = profile.getGender() == Gender.MALE ? 35 : 25;

        log.debug("BMR={}, TDEE={}, calorieGoal={}", bmr, tdee, calorieGoal);

        return new NutritionTargets((int) Math.round(calorieGoal), proteinGrams, fatGrams, carbsGrams, fiberGrams);
    }

    private double calculateBmr(UserProfile profile, double weightKg) {
        int age = calculateAge(profile.getBirthDate());
        double heightCm = profile.getHeightCm();
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return profile.getGender() == Gender.MALE ? base + 5 : base - 161;
    }

    private double calculateTdee(double bmr, ActivityLevel activityLevel) {
        double multiplier = switch (activityLevel) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case ACTIVE -> 1.725;
            case VERY_ACTIVE -> 1.9;
        };
        return bmr * multiplier;
    }

    private double applyGoalAdjustment(double tdee, GoalType goalType) {
        return switch (goalType) {
            case LOSE_WEIGHT_KEEP_MUSCLE -> tdee * 0.85;
            case LOSE_WEIGHT -> tdee * 0.80;
            case MAINTAIN -> tdee;
            case GAIN_MUSCLE -> tdee * 1.12;
        };
    }

    private int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
