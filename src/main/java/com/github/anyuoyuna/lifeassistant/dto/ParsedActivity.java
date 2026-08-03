package com.github.anyuoyuna.lifeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedActivity {
    private String date; // YYYY-MM-DD
    private String activityType;
    private Integer durationMinutes;
    private Integer estimatedCaloriesBurned; // null, если невозможно оценить
}
