package com.campusnavi.backend.studio.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.studio.document.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.dto.DocumentUpdateRequest;
import com.campusnavi.backend.studio.document.dto.ResolvedSection;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.StudioDocument;
import com.campusnavi.backend.studio.document.repository.DocumentSectionRepository;
import com.campusnavi.backend.studio.document.repository.StudioDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudioDocumentService {

    private final StudioDocumentRepository studioDocumentRepository;
    private final DocumentSectionRepository documentSectionRepository;
    private final DocumentSectionValidator sectionValidator;

    public List<DocumentSummaryResponse> getDocuments(Long memberId) {
        return studioDocumentRepository.findByMemberIdOrderByUpdatedAtDesc(memberId).stream()
                .map(DocumentSummaryResponse::from)
                .toList();
    }

    public DocumentDetailResponse getDocumentSections(Long memberId, Long documentId) {
        StudioDocument document = findOwnedDocument(memberId, documentId);
        List<DocumentSection> sections = documentSectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId);
        return DocumentDetailResponse.of(document, sections);
    }

    @Transactional
    public void updateSections(Long memberId, Long documentId, DocumentUpdateRequest request) {
        StudioDocument document = findOwnedDocument(memberId, documentId);

        List<ResolvedSection> resolvedSections =
                sectionValidator.resolve(document.getDocumentType(), request.sections());

        Map<String, DocumentSection> existingSections =
                documentSectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId).stream()
                        .collect(Collectors.toMap(DocumentSection::getSectionKey, Function.identity()));

        List<DocumentSection> newSections = new ArrayList<>();
        for (ResolvedSection resolved : resolvedSections) {
            DocumentSection existing = existingSections.get(resolved.spec().key());
            if (existing != null) {
                existing.updateContent(resolved.content());
            } else {
                newSections.add(DocumentSection.create(
                        document, resolved.spec().key(), resolved.spec().sortOrder(), resolved.content()));
            }
        }
        documentSectionRepository.saveAll(newSections);

        document.touch();
    }

    private StudioDocument findOwnedDocument(Long memberId, Long documentId) {
        return studioDocumentRepository.findById(documentId)
                .filter(doc -> doc.getMember().getId().equals(memberId))
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));
    }
}
