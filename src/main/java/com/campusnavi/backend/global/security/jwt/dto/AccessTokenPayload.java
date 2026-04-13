package com.campusnavi.backend.global.security.jwt.dto;

public record AccessTokenPayload(
        Long memberId,
        String role,
        String jti
) {
}
