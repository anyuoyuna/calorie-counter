package com.github.anyuoyuna.lifeassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedMealResponse {
    private String date; // формат YYYY-MM-DD
    private String meal; // "breakfast" | "lunch" | "dinner" | "snack"
    private List<ParsedFoodItem> items;
    private List<String> notes;
}
