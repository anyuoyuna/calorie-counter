package com.github.anyuoyuna.caloriecounter.service;

import com.github.anyuoyuna.caloriecounter.exception.GeminiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient {

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final RestClient restClient;
    private final String apiKey;

    public GeminiClient(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public String generateContent(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpServerErrorException.ServiceUnavailable lastError = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri(BASE_URL + "?key=" + apiKey)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(Map.class);

                return extractText(response);

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                lastError = e;
                log.warn("Gemini перегружена (503), попытка {}/{}", attempt, MAX_RETRIES);
                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY_MS * attempt);
                }
            } catch (Exception e) {
                log.error("Ошибка при обращении к Gemini API", e);
                return null;
            }
        }

        log.error("Gemini недоступна после {} попыток", MAX_RETRIES, lastError);
        throw new GeminiUnavailableException("Gemini API недоступна после " + MAX_RETRIES + " попыток", lastError);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) return null;

        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) return null;

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        return (String) parts.get(0).get("text");
    }
}
