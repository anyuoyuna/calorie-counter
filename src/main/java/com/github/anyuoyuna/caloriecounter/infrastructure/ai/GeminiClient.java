package com.github.anyuoyuna.caloriecounter.infrastructure.ai;

import com.github.anyuoyuna.caloriecounter.exception.GeminiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient implements AiClient {

    private final RestClient restClient;
    private final String apiKey;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    public GeminiClient(RestClient restClient, @Value("${gemini.api.key}") String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public String generateContent(String prompt) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        HttpServerErrorException.ServiceUnavailable last503Error = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> response = restClient.post()
                        .uri(GEMINI_URL + "?key=" + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(Map.class);

                String rawText = extractText(response);
                return stripMarkdownFences(rawText);

            } catch (HttpServerErrorException.ServiceUnavailable e) {
                last503Error = e;
                log.warn("Gemini перегружена (503), попытка {}/{}", attempt, MAX_RETRIES);
                if (attempt < MAX_RETRIES) {
                    sleep(RETRY_DELAY_MS * attempt); // С каждой попыткой ждем чуть дольше
                }
            } catch (Exception e) {
                log.error("Ошибка при обращении к Gemini: {}", e.getMessage());
                return null;
            }
        }

        throw new GeminiUnavailableException("Gemini сейчас недоступна, попробуй позже", last503Error);
    }

    private String extractText(Map<String, Object> response) {
        try {
            if (response == null) return null;
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return null;
        }
    }

    private String stripMarkdownFences(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-z]*\\s*", "");
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
    }
}