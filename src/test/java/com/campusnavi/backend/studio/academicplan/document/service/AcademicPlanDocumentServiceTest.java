package com.campusnavi.backend.studio.academicplan.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.studio.academicplan.document.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.document.dto.SectionInput;
import com.campusnavi.backend.studio.academicplan.document.entity.AcademicPlanMetadata;
import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanTargetService;
import com.campusnavi.backend.studio.academicplan.service.ResolvedTarget;
import com.campusnavi.backend.studio.document.entity.DocumentSection;
import com.campusnavi.backend.studio.document.entity.DocumentStatus;
import com.campusnavi.backend.studio.document.entity.DocumentType;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AcademicPlanDocumentServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AcademicPlanTargetService academicPlanTargetService;

    @Mock
    private StudioDocumentRepository studioDocumentRepository;

    @Mock
    private DocumentSectionRepository documentSectionRepository;

    @InjectMocks
    private AcademicPlanDocumentService academicPlanDocumentService;

    @Captor
    private ArgumentCaptor<StudioDocument> documentCaptor;

    @Captor
    private ArgumentCaptor<List<DocumentSection>> sectionsCaptor;

    private static final Long MEMBER_ID = 1L;
    private static final Long TARGET_ID = 5L;

    private DocumentCreateRequest request(MajorType majorType, List<SectionInput> sections) {
        return new DocumentCreateRequest(majorType, TARGET_ID, sections);
    }

    @Nested
    @DisplayName("학업계획서 생성")
    class Create {

        @Test
        @DisplayName("정상 생성 시 자격 검증한 대상명으로 문서와 섹션을 저장한다")
        void success() {
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));
            given(academicPlanTargetService.resolveAllowedTarget(any(Member.class), eq(MajorType.DOUBLE_MAJOR), eq(TARGET_ID)))
                    .willReturn(new ResolvedTarget("서울캠퍼스", "경제학과"));
            given(studioDocumentRepository.save(any(StudioDocument.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            academicPlanDocumentService.create(MEMBER_ID,
                    request(MajorType.DOUBLE_MAJOR, List.of(new SectionInput("application_motive", "내용"))));

            then(studioDocumentRepository).should().save(documentCaptor.capture());
            StudioDocument saved = documentCaptor.getValue();
            assertThat(saved.getDocumentType()).isEqualTo(DocumentType.ACADEMIC_PLAN);
            assertThat(saved.getStatus()).isEqualTo(DocumentStatus.DRAFT);
            AcademicPlanMetadata metadata = (AcademicPlanMetadata) saved.getMetadata();
            assertThat(metadata.majorType()).isEqualTo(MajorType.DOUBLE_MAJOR);
            assertThat(metadata.campusName()).isEqualTo("서울캠퍼스");
            assertThat(metadata.targetName()).isEqualTo("경제학과");

            then(documentSectionRepository).should().saveAll(sectionsCaptor.capture());
            List<DocumentSection> sections = sectionsCaptor.getValue();
            assertThat(sections).hasSize(1);
            DocumentSection section = sections.getFirst();
            assertThat(section.getSectionKey()).isEqualTo("application_motive");
            assertThat(section.getSortOrder()).isEqualTo(1);
            assertThat(section.getContent()).isEqualTo("내용");
        }

        @Test
        @DisplayName("알 수 없는 sectionKey면 STUDIO_SECTION_KEY_INVALID 예외를 던진다")
        void invalidSectionKey() {
            assertThatThrownBy(() -> academicPlanDocumentService.create(MEMBER_ID,
                    request(MajorType.DOUBLE_MAJOR, List.of(new SectionInput("unknown_key", "내용")))))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_KEY_INVALID));
        }

        @Test
        @DisplayName("섹션 길이가 maxLength를 초과하면 STUDIO_SECTION_TOO_LONG 예외를 던진다")
        void sectionTooLong() {
            assertThatThrownBy(() -> academicPlanDocumentService.create(MEMBER_ID,
                    request(MajorType.DOUBLE_MAJOR, List.of(new SectionInput("application_motive", "a".repeat(1001))))))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_TOO_LONG));
        }

        @Test
        @DisplayName("중복 sectionKey면 STUDIO_SECTION_KEY_INVALID 예외를 던진다")
        void duplicateSectionKey() {
            assertThatThrownBy(() -> academicPlanDocumentService.create(MEMBER_ID,
                    request(MajorType.DOUBLE_MAJOR, List.of(
                            new SectionInput("application_motive", "내용1"),
                            new SectionInput("application_motive", "내용2")))))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_KEY_INVALID));
        }

        @Test
        @DisplayName("탈퇴·미존재 회원이면 MEMBER_NOT_FOUND 예외를 던진다")
        void memberNotFound() {
            given(memberRepository.findProfileById(MEMBER_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> academicPlanDocumentService.create(MEMBER_ID,
                    request(MajorType.DOUBLE_MAJOR, List.of(new SectionInput("application_motive", "내용")))))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
        }
    }
}
