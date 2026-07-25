package com.github.anyuoyuna.caloriecounter.infrastructure.ai;

/**
 * Port for requesting text generation from an AI model.
 */
public interface AiClient {

    String generateContent(String prompt);
}
