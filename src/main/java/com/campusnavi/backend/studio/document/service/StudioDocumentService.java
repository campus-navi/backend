package com.campusnavi.backend.studio.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.studio.document.controller.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.StudioDocument;
import com.campusnavi.backend.studio.document.repository.DocumentSectionRepository;
import com.campusnavi.backend.studio.document.repository.StudioDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioDocumentService {

    private final StudioDocumentRepository studioDocumentRepository;
    private final DocumentSectionRepository documentSectionRepository;

    public List<DocumentSummaryResponse> getDocuments(Long memberId) {
        return studioDocumentRepository.findByMemberIdOrderByUpdatedAtDesc(memberId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    public DocumentDetailResponse getDocumentSections(Long memberId, Long documentId) {
        StudioDocument document = studioDocumentRepository.findById(documentId)
                .filter(doc -> doc.getMember().getId().equals(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));
        List<DocumentSection> sections = documentSectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId);
        return DocumentDetailResponse.of(document, sections);
    }
}
