package com.campusnavi.backend.studio.document.controller.dto;

import com.campusnavi.backend.studio.document.entity.DocumentMetadata;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.StudioDocument;

import java.time.LocalDateTime;

public record DocumentSummaryResponse(
        Long id,
        DocumentType documentType,
        DocumentStatus status,
        DocumentMetadata metadata,
        LocalDateTime updatedAt
) {
    public static DocumentSummaryResponse from(StudioDocument document) {
        return new DocumentSummaryResponse(
                document.getId(),
                document.getDocumentType(),
                document.getStatus(),
                document.getMetadata(),
                document.getUpdatedAt()
        );
    }
}
