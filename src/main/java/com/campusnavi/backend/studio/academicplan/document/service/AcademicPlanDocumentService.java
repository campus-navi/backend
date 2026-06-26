package com.campusnavi.backend.studio.academicplan.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.studio.academicplan.document.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.document.dto.SectionInput;
import com.campusnavi.backend.studio.academicplan.document.entity.AcademicPlanMetadata;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
import com.campusnavi.backend.studio.academicplan.service.ResolvedTarget;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.SectionSpec;
import com.campusnavi.backend.studio.document.entity.StudioDocument;
import com.campusnavi.backend.studio.document.repository.DocumentSectionRepository;
import com.campusnavi.backend.studio.document.repository.StudioDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPlanDocumentService {

    private final MemberRepository memberRepository;
    private final AcademicPlanTargetService academicPlanTargetService;
    private final StudioDocumentRepository studioDocumentRepository;
    private final DocumentSectionRepository documentSectionRepository;

    @Transactional
    public void create(Long memberId, DocumentCreateRequest request) {
        validateSections(request.sections());

        Member member = memberRepository.findProfileById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        ResolvedTarget target = academicPlanTargetService.resolveAllowedTarget(
                member, request.majorType(), request.targetId());
        AcademicPlanMetadata metadata = new AcademicPlanMetadata(
                request.majorType(), target.campusName(), target.targetName());

        StudioDocument document = studioDocumentRepository.save(
                StudioDocument.create(member, DocumentType.ACADEMIC_PLAN, metadata));

        List<DocumentSection> sections = request.sections().stream()
                .map(input -> {
                    SectionSpec spec = sectionSpec(input.sectionKey());
                    return DocumentSection.create(document, spec.key(), spec.sortOrder(), input.content());
                })
                .toList();
        documentSectionRepository.saveAll(sections);
    }

    private void validateSections(List<SectionInput> inputs) {
        Set<String> seenKeys = new HashSet<>();
        for (SectionInput input : inputs) {
            SectionSpec spec = sectionSpec(input.sectionKey());
            if (input.content().length() > spec.maxLength()) {
                throw new BusinessException(ErrorCode.STUDIO_SECTION_TOO_LONG);
            }
            if (!seenKeys.add(input.sectionKey())) {
                throw new BusinessException(ErrorCode.STUDIO_SECTION_KEY_INVALID);
            }
        }
    }

    private SectionSpec sectionSpec(String key) {
        return DocumentType.ACADEMIC_PLAN.findSection(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDIO_SECTION_KEY_INVALID));
    }
}
