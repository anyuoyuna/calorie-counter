package com.github.anyuoyuna.lifeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantIntent(
        String primaryIntent, // "FOOD", "ACTIVITY", "QUESTION", "GREETING", "UNKNOWN", FINANCE
        List<String> actions
) {}
