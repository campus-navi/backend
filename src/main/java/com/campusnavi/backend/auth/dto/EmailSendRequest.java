package com.campusnavi.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EmailSendRequest(
        @NotNull
        Long campusId,
        @NotBlank
        @Email
        String email
) {
}
