package com.github.anyuoyuna.caloriecounter.service;

/**
 * Port for requesting text generation from an AI model.
 */
public interface AiClient {

    String generateContent(String prompt);
}
