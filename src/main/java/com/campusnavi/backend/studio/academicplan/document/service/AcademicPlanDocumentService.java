package com.campusnavi.backend.studio.academicplan.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.studio.academicplan.document.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.document.entity.AcademicPlanMetadata;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
import com.campusnavi.backend.studio.academicplan.service.ResolvedTarget;
import com.campusnavi.backend.studio.document.dto.ResolvedSection;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.StudioDocument;
import com.campusnavi.backend.studio.document.repository.DocumentSectionRepository;
import com.campusnavi.backend.studio.document.repository.StudioDocumentRepository;
import com.campusnavi.backend.studio.document.service.DocumentSectionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicPlanDocumentService {

    private final MemberRepository memberRepository;
    private final AcademicPlanTargetService academicPlanTargetService;
    private final StudioDocumentRepository studioDocumentRepository;
    private final DocumentSectionRepository documentSectionRepository;
    private final DocumentSectionValidator sectionValidator;

    @Transactional
    public void create(Long memberId, DocumentCreateRequest request) {
        List<ResolvedSection> resolvedSections =
                sectionValidator.resolve(DocumentType.ACADEMIC_PLAN, request.sections());

        Member member = memberRepository.findProfileById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        ResolvedTarget target = academicPlanTargetService.resolveAllowedTarget(
                member, request.majorType(), request.targetId());
        AcademicPlanMetadata metadata = new AcademicPlanMetadata(
                request.majorType(), target.campusName(), target.targetName());

        StudioDocument document = studioDocumentRepository.save(
                StudioDocument.create(member, DocumentType.ACADEMIC_PLAN, metadata));

        List<DocumentSection> sections = resolvedSections.stream()
                .map(r -> DocumentSection.create(document, r.spec().key(), r.spec().sortOrder(), r.content()))
                .toList();
        documentSectionRepository.saveAll(sections);
    }
}
