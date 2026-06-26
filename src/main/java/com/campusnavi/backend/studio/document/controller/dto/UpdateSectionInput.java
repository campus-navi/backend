package com.campusnavi.backend.studio.document.controller.dto;

import com.campusnavi.backend.studio.document.dto.SectionContent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateSectionInput(
        @Schema(description = "섹션 키", example = "study_plan")
        @NotBlank String sectionKey,
        @NotBlank String content
) implements SectionContent {
}
