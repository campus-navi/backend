package com.campusnavi.backend.studio.academicplan.document.dto;

import com.campusnavi.backend.studio.academicplan.entity.MajorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DocumentCreateRequest(
        @NotNull MajorType majorType,
        @NotNull Long targetId,
        @NotEmpty @Valid List<SectionInput> sections
) {
}
