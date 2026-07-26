package com.github.anyuoyuna.caloriecounter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantIntent(
        String primaryIntent, // "FOOD", "ACTIVITY", "QUESTION", "GREETING", "UNKNOWN"
        List<String> actions  // Список действий, если их несколько
) {}
