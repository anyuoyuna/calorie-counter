package com.github.anyuoyuna.lifeassistant.dto;

import java.math.BigDecimal;

public record ParsedExpense(
        BigDecimal amount,
        String category,
        String description,
        String type
) {}