package com.campusnavi.backend.auth.dto;

public record EmailVerifyRequest(
        String email,
        String code
) {
}
