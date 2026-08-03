package com.github.anyuoyuna.lifeassistant.domain.activity;

import com.github.anyuoyuna.lifeassistant.dto.ParsedActivity;
import com.github.anyuoyuna.lifeassistant.entity.User;
import com.github.anyuoyuna.lifeassistant.entity.WeightLog;
import com.github.anyuoyuna.lifeassistant.infrastructure.ai.GeneralAiAssistant;
import com.github.anyuoyuna.lifeassistant.repository.WeightLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Service
public class ActivityParsingService {

    private final GeneralAiAssistant aiAssistant;
    private final WeightLogRepository weightLogRepo;
    private final Clock clock;

    public ActivityParsingService(GeneralAiAssistant aiAssistant,
                                  WeightLogRepository weightLogRepo,
                                  Clock clock) {
        this.aiAssistant = aiAssistant;
        this.weightLogRepo = weightLogRepo;
        this.clock = clock;
    }

    public ParsedActivity parse(User user, String userText) {
        double weightKg = weightLogRepo.findFirstByUserOrderByLoggedAtDesc(user)
                .map(WeightLog::getWeightKg)
                .orElse(70.0);

        String today = LocalDate.now(clock).toString();

        return aiAssistant.parseActivity(userText, weightKg, today);
    }
}
