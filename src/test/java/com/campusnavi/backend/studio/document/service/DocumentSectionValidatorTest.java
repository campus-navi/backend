package com.campusnavi.backend.studio.document.service;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.studio.document.dto.ResolvedSection;
import com.campusnavi.backend.studio.document.dto.SectionContent;
import com.campusnavi.backend.studio.document.entity.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentSectionValidatorTest {

    private final DocumentSectionValidator validator = new DocumentSectionValidator();

    private record TestSection(String sectionKey, String content) implements SectionContent {
    }

    @Test
    @DisplayName("유효한 섹션이면 spec과 content를 매핑해 반환한다")
    void resolveSuccess() {
        List<ResolvedSection> resolved = validator.resolve(DocumentType.ACADEMIC_PLAN,
                List.of(new TestSection("application_motive", "내용")));

        assertThat(resolved).hasSize(1);
        ResolvedSection section = resolved.getFirst();
        assertThat(section.spec().key()).isEqualTo("application_motive");
        assertThat(section.spec().sortOrder()).isEqualTo(1);
        assertThat(section.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("알 수 없는 sectionKey면 STUDIO_SECTION_KEY_INVALID 예외를 던진다")
    void invalidSectionKey() {
        assertThatThrownBy(() -> validator.resolve(DocumentType.ACADEMIC_PLAN,
                List.of(new TestSection("unknown_key", "내용"))))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_KEY_INVALID));
    }

    @Test
    @DisplayName("섹션 길이가 maxLength를 초과하면 STUDIO_SECTION_TOO_LONG 예외를 던진다")
    void sectionTooLong() {
        assertThatThrownBy(() -> validator.resolve(DocumentType.ACADEMIC_PLAN,
                List.of(new TestSection("application_motive", "a".repeat(1001)))))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_TOO_LONG));
    }

    @Test
    @DisplayName("중복 sectionKey면 STUDIO_SECTION_KEY_INVALID 예외를 던진다")
    void duplicateSectionKey() {
        assertThatThrownBy(() -> validator.resolve(DocumentType.ACADEMIC_PLAN,
                List.of(new TestSection("application_motive", "내용1"),
                        new TestSection("application_motive", "내용2"))))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STUDIO_SECTION_KEY_INVALID));
    }
}
