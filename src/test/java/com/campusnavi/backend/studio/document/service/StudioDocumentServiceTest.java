package com.campusnavi.backend.studio.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.studio.document.controller.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentUpdateRequest;
import com.campusnavi.backend.studio.document.controller.dto.SectionResponse;
import com.campusnavi.backend.studio.document.controller.dto.UpdateSectionInput;
import com.campusnavi.backend.studio.document.dto.ResolvedSection;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import com.campusnavi.backend.studio.document.entity.SectionSpec;
import com.campusnavi.backend.studio.document.entity.StudioDocument;
import com.campusnavi.backend.studio.document.repository.DocumentSectionRepository;
import com.campusnavi.backend.studio.document.repository.StudioDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class StudioDocumentServiceTest {

    @Mock
    private StudioDocumentRepository studioDocumentRepository;

    @Mock
    private DocumentSectionRepository documentSectionRepository;

    @Mock
    private DocumentSectionValidator sectionValidator;

    @InjectMocks
    private StudioDocumentService studioDocumentService;

    @Captor
    private ArgumentCaptor<List<DocumentSection>> sectionsCaptor;

    private static final Long MEMBER_ID = 1L;
    private static final Long DOCUMENT_ID = 10L;

    @Nested
    @DisplayName("목록 조회")
    class GetDocuments {

        @Test
        @DisplayName("내 문서를 요약 목록으로 반환한다")
        void success() {
            Object metadata = Map.of("majorType", "DOUBLE_MAJOR", "targetName", "경제학과");
            LocalDateTime updatedAt = LocalDateTime.now();
            StudioDocument document = mock(StudioDocument.class);
            given(document.getId()).willReturn(DOCUMENT_ID);
            given(document.getDocumentType()).willReturn(DocumentType.ACADEMIC_PLAN);
            given(document.getStatus()).willReturn(DocumentStatus.DRAFT);
            given(document.getMetadata()).willReturn(metadata);
            given(document.getUpdatedAt()).willReturn(updatedAt);
            given(studioDocumentRepository.findByMemberIdOrderByUpdatedAtDesc(MEMBER_ID))
                    .willReturn(List.of(document));

            List<DocumentSummaryResponse> result = studioDocumentService.getDocuments(MEMBER_ID);

            assertThat(result).hasSize(1);
            DocumentSummaryResponse summary = result.getFirst();
            assertThat(summary.id()).isEqualTo(DOCUMENT_ID);
            assertThat(summary.documentType()).isEqualTo(DocumentType.ACADEMIC_PLAN);
            assertThat(summary.status()).isEqualTo(DocumentStatus.DRAFT);
            assertThat(summary.metadata()).isEqualTo(metadata);
            assertThat(summary.updatedAt()).isEqualTo(updatedAt);
        }
    }

    @Nested
    @DisplayName("원문 섹션 조회")
    class GetDocumentSections {

        @Test
        @DisplayName("본인 문서면 섹션을 sortOrder 순으로 반환한다")
        void success() {
            Member member = mock(Member.class);
            given(member.getId()).willReturn(MEMBER_ID);
            StudioDocument document = mock(StudioDocument.class);
            given(document.getMember()).willReturn(member);
            given(document.getId()).willReturn(DOCUMENT_ID);
            given(document.getDocumentType()).willReturn(DocumentType.ACADEMIC_PLAN);
            given(document.getStatus()).willReturn(DocumentStatus.DRAFT);
            given(studioDocumentRepository.findById(DOCUMENT_ID)).willReturn(Optional.of(document));
            DocumentSection section = mock(DocumentSection.class);
            given(section.getSectionKey()).willReturn("application_motive");
            given(section.getContent()).willReturn("내용");
            given(section.getSortOrder()).willReturn(1);
            given(documentSectionRepository.findByDocumentIdOrderBySortOrderAsc(DOCUMENT_ID))
                    .willReturn(List.of(section));

            DocumentDetailResponse result = studioDocumentService.getDocumentSections(MEMBER_ID, DOCUMENT_ID);

            assertThat(result.id()).isEqualTo(DOCUMENT_ID);
            assertThat(result.documentType()).isEqualTo(DocumentType.ACADEMIC_PLAN);
            assertThat(result.sections()).hasSize(1);
            SectionResponse sectionResponse = result.sections().getFirst();
            assertThat(sectionResponse.sectionKey()).isEqualTo("application_motive");
            assertThat(sectionResponse.content()).isEqualTo("내용");
            assertThat(sectionResponse.sortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("타인 문서면 STUDIO_DOCUMENT_NOT_FOUND 예외를 던진다")
        void notOwner() {
            Member member = mock(Member.class);
            given(member.getId()).willReturn(999L);
            StudioDocument document = mock(StudioDocument.class);
            given(document.getMember()).willReturn(member);
            given(studioDocumentRepository.findById(DOCUMENT_ID)).willReturn(Optional.of(document));

            assertThatThrownBy(() -> studioDocumentService.getDocumentSections(MEMBER_ID, DOCUMENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 문서면 STUDIO_DOCUMENT_NOT_FOUND 예외를 던진다")
        void notFound() {
            given(studioDocumentRepository.findById(DOCUMENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> studioDocumentService.getDocumentSections(MEMBER_ID, DOCUMENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("섹션 이어쓰기")
    class UpdateSections {

        @Test
        @DisplayName("기존 섹션은 수정하고 미작성 섹션은 추가하며 문서 수정시각을 갱신한다")
        void upsert() {
            Member member = mock(Member.class);
            given(member.getId()).willReturn(MEMBER_ID);
            StudioDocument document = mock(StudioDocument.class);
            given(document.getMember()).willReturn(member);
            given(document.getDocumentType()).willReturn(DocumentType.ACADEMIC_PLAN);
            given(studioDocumentRepository.findById(DOCUMENT_ID)).willReturn(Optional.of(document));

            given(sectionValidator.resolve(eq(DocumentType.ACADEMIC_PLAN), any())).willReturn(List.of(
                    new ResolvedSection(new SectionSpec("application_motive", 1000, true, 1), "수정된 내용"),
                    new ResolvedSection(new SectionSpec("study_plan", 1000, true, 3), "새 내용")));

            DocumentSection existing = mock(DocumentSection.class);
            given(existing.getSectionKey()).willReturn("application_motive");
            given(documentSectionRepository.findByDocumentIdOrderBySortOrderAsc(DOCUMENT_ID))
                    .willReturn(List.of(existing));

            studioDocumentService.updateSections(MEMBER_ID, DOCUMENT_ID, new DocumentUpdateRequest(List.of(
                    new UpdateSectionInput("application_motive", "수정된 내용"),
                    new UpdateSectionInput("study_plan", "새 내용"))));

            then(existing).should().updateContent("수정된 내용");
            then(documentSectionRepository).should().saveAll(sectionsCaptor.capture());
            List<DocumentSection> newSections = sectionsCaptor.getValue();
            assertThat(newSections).hasSize(1);
            assertThat(newSections.getFirst().getSectionKey()).isEqualTo("study_plan");
            then(document).should().touch();
        }

        @Test
        @DisplayName("타인 문서면 STUDIO_DOCUMENT_NOT_FOUND 예외를 던진다")
        void notOwner() {
            Member member = mock(Member.class);
            given(member.getId()).willReturn(999L);
            StudioDocument document = mock(StudioDocument.class);
            given(document.getMember()).willReturn(member);
            given(studioDocumentRepository.findById(DOCUMENT_ID)).willReturn(Optional.of(document));

            assertThatThrownBy(() -> studioDocumentService.updateSections(MEMBER_ID, DOCUMENT_ID,
                    new DocumentUpdateRequest(List.of(new UpdateSectionInput("study_plan", "내용")))))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_DOCUMENT_NOT_FOUND));
        }
    }
}
