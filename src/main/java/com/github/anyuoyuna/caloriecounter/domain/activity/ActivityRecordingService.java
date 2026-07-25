package com.github.anyuoyuna.caloriecounter.domain.activity;

import com.github.anyuoyuna.caloriecounter.dto.ParsedActivity;
import com.github.anyuoyuna.caloriecounter.entity.ActivityLog;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.repository.ActivityLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
public class ActivityRecordingService {

    private final ActivityLogRepository activityLogRepo;
    private final Clock clock;

    public ActivityRecordingService(ActivityLogRepository activityLogRepo, Clock clock) {
        this.activityLogRepo = activityLogRepo;
        this.clock = clock;
    }

    @Transactional
    public ActivityLog recordActivity(User user, ParsedActivity parsed) {
        LocalDate date = parseDateOrToday(parsed.getDate());

        ActivityLog activityLog = new ActivityLog();
        activityLog.setUser(user);
        activityLog.setActivityDate(date);
        activityLog.setActivityType(parsed.getActivityType());
        activityLog.setDurationMinutes(parsed.getDurationMinutes());
        activityLog.setEstimatedCaloriesBurned(parsed.getEstimatedCaloriesBurned());

        activityLogRepo.save(activityLog);

        log.info("Записана активность для пользователя {}: {} ({} мин, {} ккал)",
                user.getTelegramId(), parsed.getActivityType(), parsed.getDurationMinutes(),
                parsed.getEstimatedCaloriesBurned());

        return activityLog;
    }

    private LocalDate parseDateOrToday(String dateStr) {
        try {
            return dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now(clock);
        } catch (Exception e) {
            return LocalDate.now(clock);
        }
    }
}
