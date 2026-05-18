package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GradeUpdateRequest(
        @NotNull
        @Min(1) @Max(4)
        Integer grade
) {
}
