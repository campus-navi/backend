package com.campusnavi.backend.studio.academicplan.dto;

import com.campusnavi.backend.studio.document.dto.SectionContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SectionInput(
        @Schema(description = "섹션 키 (지원동기=application_motive, 관심분야=interest_field, 학업계획=study_plan, 기타=academic_plan_etc)",
                allowableValues = {"application_motive", "interest_field", "study_plan", "academic_plan_etc"},
                example = "application_motive")
        @NotBlank String sectionKey,
        @NotBlank String content
) implements SectionContent {
}
