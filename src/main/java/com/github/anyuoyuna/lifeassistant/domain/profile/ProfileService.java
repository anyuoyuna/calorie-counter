package com.github.anyuoyuna.lifeassistant.domain.profile;

import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.UserProfile;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
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
                .orElseThrow(() -> new IllegalStateException("Профиль не найден, нужен онбординг"));

        Optional<WeightLog> latestOpt = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user);
        Optional<WeightLog> firstOpt = weightLogRepo.findFirstByUserOrderByLoggedAtAsc(user);

        int age = Period.between(profile.getBirthDate(), LocalDate.now()).getYears();

        StringBuilder sb = new StringBuilder();
        sb.append("Профиль\n\n");
        sb.append("Пол: ").append(genderLabel(profile.getGender())).append("\n");
        sb.append("Возраст: ").append(age).append(" лет\n");
        sb.append("Рост: ").append(profile.getHeightCm()).append(" см\n\n");

        if (latestOpt.isPresent()) {
            WeightLog latest = latestOpt.get();
            sb.append("Текущий вес: ").append(latest.getWeightKg()).append(" кг")
                    .append(" (на ").append(latest.getLoggedAt().format(DATE_FMT)).append(")\n");
            sb.append("Процент жира: ").append(formatOrUnknown(latest.getBodyFatPercent())).append("\n");
            sb.append("Вес мышц: ").append(formatWeightOrUnknown(latest.getMuscleWeight())).append("\n");

            if (firstOpt.isPresent() && !firstOpt.get().getId().equals(latest.getId())) {
                double diff = latest.getWeightKg() - firstOpt.get().getWeightKg();
                String arrow = diff < 0 ? "↓" : (diff > 0 ? "↑" : "→");
                sb.append(String.format("Прогресс с %s: %s %.1f кг%n",
                        firstOpt.get().getLoggedAt().format(DATE_FMT), arrow, Math.abs(diff)));
            }
            sb.append("\n");
        }

        sb.append("Цель: ").append(goalLabel(profile.getGoalType())).append("\n");
        sb.append("Активность: ").append(activityLabel(profile.getActivityLevel())).append("\n");
        if (profile.getTargetWeightKg() != null) {
            sb.append("Желаемый вес: ").append(profile.getTargetWeightKg()).append(" кг\n");
        }
        sb.append("\nДневная цель по калориям: ").append(user.getDailyCalorieGoal()).append(" ккал");

        return sb.toString();
    }

    private String formatOrUnknown(Double value) {
        return value != null ? value + "%" : "не указано";
    }

    private String formatWeightOrUnknown(Double value) {
        return value != null ? value + " кг" : "не указано";
    }

    private String genderLabel(com.github.anyuoyuna.lifeassistant.entity.enums.Gender g) {
        return g.name().equals("MALE") ? "мужской" : "женский";
    }

    private String goalLabel(com.github.anyuoyuna.lifeassistant.entity.enums.GoalType g) {
        return switch (g) {
            case LOSE_WEIGHT_KEEP_MUSCLE -> "похудение с сохранением мышц";
            case LOSE_WEIGHT -> "похудение";
            case MAINTAIN -> "поддержание веса";
            case GAIN_MUSCLE -> "набор массы";
        };
    }

    private String activityLabel(com.github.anyuoyuna.lifeassistant.entity.enums.ActivityLevel a) {
        return switch (a) {
            case SEDENTARY -> "сидячий";
            case LIGHT -> "лёгкий";
            case MODERATE -> "умеренный";
            case ACTIVE -> "высокий";
            case VERY_ACTIVE -> "очень высокий";
        };
    }
}
