package com.campusnavi.backend.studio.document.dto;

import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.StudioDocument;

import java.util.List;

public record DocumentDetailResponse(
        Long id,
        DocumentType documentType,
        DocumentStatus status,
        List<SectionResponse> sections
) {
    public static DocumentDetailResponse of(StudioDocument document, List<DocumentSection> sections) {
        return new DocumentDetailResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                sections.stream().map(SectionResponse::from).toList()
        );
    }
}
