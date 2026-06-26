package com.campusnavi.backend.studio.academicplan.document.dto;

import jakarta.validation.constraints.NotBlank;

public record SectionInput(
        @NotBlank String sectionKey,
        @NotBlank String content
) {
}
