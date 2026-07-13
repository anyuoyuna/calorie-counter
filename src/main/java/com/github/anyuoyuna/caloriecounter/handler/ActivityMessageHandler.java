package com.github.anyuoyuna.caloriecounter.handler;

import com.github.anyuoyuna.caloriecounter.bot.BotResponse;
import com.github.anyuoyuna.caloriecounter.dto.ParsedActivity;
import com.github.anyuoyuna.caloriecounter.entity.ActivityLog;
import com.github.anyuoyuna.caloriecounter.entity.User;
import com.github.anyuoyuna.caloriecounter.service.ActivityParsingService;
import com.github.anyuoyuna.caloriecounter.service.ActivityRecordingService;
import org.springframework.stereotype.Component;

@Component
public class ActivityMessageHandler {

    private static final double EAT_BACK_RATIO = 0.5;

    private final ActivityParsingService activityParsingService;
    private final ActivityRecordingService activityRecordingService;

    public ActivityMessageHandler(ActivityParsingService activityParsingService,
                                  ActivityRecordingService activityRecordingService) {
        this.activityParsingService = activityParsingService;
        this.activityRecordingService = activityRecordingService;
    }

    public BotResponse handle(User user, String text) {
        ParsedActivity parsed = activityParsingService.parse(user, text);

        if (parsed == null || parsed.getActivityType() == null) {
            return BotResponse.plain("Не поняла, что за активность. Попробуй описать подробнее.");
        }

        ActivityLog saved = activityRecordingService.recordActivity(user, parsed);

        StringBuilder response = new StringBuilder();
        response.append("Записала: ").append(saved.getActivityType());
        if (saved.getDurationMinutes() != null) {
            response.append(" (").append(saved.getDurationMinutes()).append(" мин)");
        }
        if (saved.getEstimatedCaloriesBurned() != null) {
            int bonus = (int) Math.round(saved.getEstimatedCaloriesBurned() * EAT_BACK_RATIO);
            response.append("\nПримерный расход: ").append(saved.getEstimatedCaloriesBurned()).append(" ккал");
            response.append("\nЭто добавит ~").append(bonus).append(" ккал к норме на сегодня.");
        } else {
            response.append("\nНе смогла оценить расход калорий для этой активности.");
        }

        return BotResponse.plain(response.toString());
    }
}
