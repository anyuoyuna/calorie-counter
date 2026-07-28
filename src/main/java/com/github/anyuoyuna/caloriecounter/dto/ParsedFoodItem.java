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
    private Double calories;
    private Double protein;
    private Double fat;
    private Double carbs;
    private Double fiber;

    public boolean isRecognized() {
        return calories != null && grams != null;
    }
}
