package com.github.anyuoyuna.lifeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssistantIntent(
        String primaryIntent, // "FINANCE_ADD", "FINANCE_QUERY", "GREETING", or "UNKNOWN"
        List<String> actions
) {}
