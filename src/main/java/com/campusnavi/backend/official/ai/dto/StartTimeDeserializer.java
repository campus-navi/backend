package com.campusnavi.backend.official.ai.dto;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalTime;

public class StartTimeDeserializer extends ValueDeserializer<LocalTime> {

    @Override
    public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) {
        String value = p.getString();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("24:00")) {
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(value);
    }
}
