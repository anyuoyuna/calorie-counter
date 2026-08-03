package com.github.anyuoyuna.lifeassistant.domain.profile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EditSession {
    private EditStep step;
    private Double pendingWeight;
    private Double pendingBodyFat;
}