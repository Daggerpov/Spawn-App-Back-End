package com.danielagapov.spawn.Enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventCategory {
    GENERAL("General"),
    FOOD_AND_DRINK("Food and Drink"),
    ACTIVE("Active"),
    GRIND("Grind"),
    CHILL("Chill"),
    ;

    private final String value;

    EventCategory(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
} 