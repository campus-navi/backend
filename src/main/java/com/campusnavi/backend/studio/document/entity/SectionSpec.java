package com.campusnavi.backend.studio.document.entity;

public record SectionSpec(
        String key,
        int maxLength,
        boolean required,
        int sortOrder
) {
}
