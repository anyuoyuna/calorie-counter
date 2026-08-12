package com.github.anyuoyuna.lifeassistant.handler;

import com.github.anyuoyuna.lifeassistant.bot.BotResponse;
import com.github.anyuoyuna.lifeassistant.domain.activity.ActivityParsingService;
import com.github.anyuoyuna.lifeassistant.domain.activity.ActivityRecordingService;
import com.github.anyuoyuna.lifeassistant.dto.ParsedActivity;
import com.github.anyuoyuna.lifeassistant.entity.ActivityLog;
import com.github.anyuoyuna.lifeassistant.entity.User;
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
            return BotResponse.plain("Didn't recognize this activity. Try describing it in more detail.");
        }
        ActivityLog saved = activityRecordingService.recordActivity(user, parsed);

        StringBuilder response = new StringBuilder();
        response.append("Logged: ").append(saved.getActivityType());
        if (saved.getDurationMinutes() != null) {
            response.append(" (").append(saved.getDurationMinutes()).append(" min)");
        }
        if (saved.getEstimatedCaloriesBurned() != null) {
            int bonus = (int) Math.round(saved.getEstimatedCaloriesBurned() * EAT_BACK_RATIO);
            response.append("\nEstimated burn: ").append(saved.getEstimatedCaloriesBurned()).append(" kcal");
            response.append("\nThis will add ~").append(bonus).append(" kcal to today's limit.");
        } else {
            response.append("\nCould not estimate calorie burn for this activity.");
        }
        return BotResponse.plain(response.toString());
    }
}
