package com.github.anyuoyuna.caloriecounter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EmbeddingClient {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent";
    private static final int DIMENSIONS = 768;

    private final RestClient restClient;
    private final String apiKey;

    public EmbeddingClient(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public float[] embed(String text) {
        Map<String, Object> requestBody = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "outputDimensionality", DIMENSIONS
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(URL + "?key=" + apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            return extractVector(response);
        } catch (Exception e) {
            log.error("Ошибка при получении эмбеддинга для текста: {}", text, e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private float[] extractVector(Map<String, Object> response) {
        if (response == null) return null;
        Map<String, Object> embedding = (Map<String, Object>) response.get("embedding");
        List<Double> values = (List<Double>) embedding.get("values");

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i).floatValue();
        }
        return result;
    }
}
