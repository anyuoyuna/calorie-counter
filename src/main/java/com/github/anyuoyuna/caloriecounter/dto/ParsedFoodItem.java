package com.github.anyuoyuna.caloriecounter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedFoodItem {
    private String name;
    private Double grams;
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
