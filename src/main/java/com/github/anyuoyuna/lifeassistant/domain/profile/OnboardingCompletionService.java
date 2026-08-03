package com.github.anyuoyuna.lifeassistant.domain.profile;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
import com.github.anyuoyuna.lifeassistant.onboarding.OnboardingState;
import com.github.anyuoyuna.lifeassistant.repository.UserProfileRepository;
import com.github.anyuoyuna.lifeassistant.repository.UserRepository;
import com.github.anyuoyuna.lifeassistant.repository.WeightLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
public class OnboardingCompletionService {

    private final UserRepository userRepo;
    private final UserProfileRepository profileRepo;
    private final WeightLogRepository weightLogRepo;
    private final CalorieCalculationService calorieService;

    public OnboardingCompletionService(UserRepository userRepo,
                                       UserProfileRepository profileRepo,
                                       WeightLogRepository weightLogRepo,
                                       CalorieCalculationService calorieService) {
        this.userRepo = userRepo;
        this.profileRepo = profileRepo;
        this.weightLogRepo = weightLogRepo;
        this.calorieService = calorieService;
    }

    @Transactional
    public int completeOnboarding(Long telegramId, OnboardingState state) {
        User user = userRepo.findByTelegramId(telegramId).orElseThrow();
        user.setDisplayName(state.getDisplayName());
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setGender(state.getGender());
        profile.setBirthDate(state.getBirthDate());
        profile.setHeightCm(state.getHeightCm());
        profile.setGoalType(state.getGoalType());
        profile.setActivityLevel(state.getActivityLevel());
        profile.setTargetWeightKg(state.getTargetWeightKg());
        profileRepo.save(profile);

        WeightLog weightLog = new WeightLog();
        weightLog.setUser(user);
        weightLog.setLoggedAt(LocalDate.now());
        weightLog.setWeightKg(state.getWeightKg());
        weightLog.setBodyFatPercent(state.getBodyFatPercent());
        weightLog.setMuscleWeight(state.getMuscleWeight());
        weightLogRepo.save(weightLog);

        CalorieCalculationService.NutritionTargets targets = calorieService.calculateTargets(profile, state.getWeightKg());
        user.setDailyCalorieGoal(targets.calories());
        user.setDailyProteinGoal(targets.protein());
        user.setDailyFatGoal(targets.fat());
        user.setDailyCarbsGoal(targets.carbs());
        user.setDailyFiberGoal(targets.fiber());
        userRepo.save(user);

        log.info("Онбординг завершён для пользователя {}, дневная цель = {} ккал", telegramId, targets.calories());
        return targets.calories();
    }
}
