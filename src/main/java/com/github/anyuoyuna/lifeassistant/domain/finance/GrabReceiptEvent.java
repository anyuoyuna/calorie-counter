package com.github.anyuoyuna.lifeassistant.domain.finance;

import java.io.Serializable;

public record GrabReceiptEvent(
        Long userId,
        String messageId,
        String snippet
) implements Serializable {}