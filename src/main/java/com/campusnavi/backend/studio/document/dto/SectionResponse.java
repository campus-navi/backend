package com.campusnavi.backend.studio.document.dto;

import com.campusnavi.backend.studio.document.entity.DocumentSection;

public record SectionResponse(
        String sectionKey,
        String content,
        int sortOrder
) {
    public static SectionResponse from(DocumentSection section) {
        return new SectionResponse(section.getSectionKey(), section.getContent(), section.getSortOrder());
    }
}
