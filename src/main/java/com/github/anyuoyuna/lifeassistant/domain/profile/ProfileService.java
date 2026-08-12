package com.github.anyuoyuna.lifeassistant.domain.profile;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
import com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel;
import com.github.anyuoyuna.lifeassistant.entity.enums.Gender;
import com.github.anyuoyuna.lifeassistant.entity.enums.GoalType;
import com.github.anyuoyuna.lifeassistant.repository.UserProfileRepository;
import com.github.anyuoyuna.lifeassistant.repository.WeightLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ProfileService {

    private final UserProfileRepository profileRepo;
    private final WeightLogRepository weightLogRepo;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public ProfileService(UserProfileRepository profileRepo, WeightLogRepository weightLogRepo) {
        this.profileRepo = profileRepo;
        this.weightLogRepo = weightLogRepo;
    }

    public String buildProfileSummary(User user) {
        UserProfile profile = profileRepo.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("Profile not found, onboarding required"));
        Optional<WeightLog> latestOpt = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user);
        Optional<WeightLog> firstOpt = weightLogRepo.findFirstByUserOrderByLoggedAtAsc(user);
        int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();
        StringBuilder sb = new StringBuilder();
        sb.append("Profile\n\n");
        sb.append("Sex: ").append(genderLabel(profile.getGender())).append("\n");
        sb.append("Age: ").append(age).append(" years\n");
        sb.append("Height: ").append(profile.getHeightCm()).append(" sm\n\n");
        if (latestOpt.isPresent()) {
            WeightLog latest = latestOpt.get();
            sb.append("Current weight: ").append(latest.getWeightKg()).append(" kg")
                    .append(" (as of ").append(latest.getLoggedAt().format(DATE_FMT)).append(")\n");
            sb.append("Body fat %: ").append(formatOrUnknown(latest.getBodyFatPercent())).append("\n");
            sb.append("Muscle weight: ").append(formatWeightOrUnknown(latest.getMuscleWeight())).append("\n");

            if (firstOpt.isPresent() && !firstOpt.get().getId().equals(latest.getId())) {
                double diff = latest.getWeightKg() - firstOpt.get().getWeightKg();
                String arrow = diff < 0 ? "↓" : (diff > 0 ? "↑" : "→");
                sb.append(String.format("Progress since %s: %s %.1f kg%n",
                        firstOpt.get().getLoggedAt().format(DATE_FMT), arrow, Math.abs(diff)));
            }
            sb.append("\n");
        }
        sb.append("Goal: ").append(goalLabel(profile.getGoalType())).append("\n");
        sb.append("Activity: ").append(activityLabel(profile.getActivityLevel())).append("\n");
        if (profile.getTargetWeightKg() != null) {
            sb.append("Target weight: ").append(profile.getTargetWeightKg()).append(" kg\n");
        }
        sb.append("\nDaily calorie goal: ").append(user.getDailyCalorieGoal()).append(" kcal");
        return sb.toString();
    }

    private String formatOrUnknown(Double value) {
        return value != null ? value + "%" : "not set";
    }

    private String formatWeightOrUnknown(Double value) {
        return value != null ? value + " kg" : "not set";
    }

    private String genderLabel(Gender g) {
        return g.name().equals("MALE") ? "male" : "female";
    }

    private String goalLabel(GoalType g) {
        return switch (g) {
            case LOSE_WEIGHT_KEEP_MUSCLE -> "lose weight keep muscle";
            case LOSE_WEIGHT -> "lose weight";
            case MAINTAIN -> "maintain";
            case GAIN_MUSCLE -> "gain muscle";
        };
    }

    private String activityLabel(ActivityLevel a) {
        return switch (a) {
            case SEDENTARY -> "sedentary";
            case LIGHT -> "light";
            case MODERATE -> "moderate";
            case ACTIVE -> "active";
            case VERY_ACTIVE -> "very_active";
        };
    }
}
