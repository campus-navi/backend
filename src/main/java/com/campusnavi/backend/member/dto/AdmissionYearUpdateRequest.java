package com.campusnavi.backend.member.dto;

import jakarta.validation.constraints.NotNull;

public record AdmissionYearUpdateRequest(
        @NotNull
        Integer admissionYear
) {
}
