package com.github.anyuoyuna.caloriecounter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedFoodItem {
    private String cleanName;
    private String originalInput;
    private Double grams;

    @JsonProperty("weight_source")
    private String weightSource; // "explicit" | "estimated" | "calculated"
    private Double totalCalories;
    private Double totalProtein;
    private Double totalFat;
    private Double totalCarbs;
    private Double totalFiber;

    public boolean isRecognized() {
        return totalCalories != null && grams != null;
    }
}
