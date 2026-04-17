package com.campusnavi.backend.global.security.jwt.dto;

public record AccessTokenPayload(
        Long memberId,
        Long universityId,
        String role,
        String jti,
        long remainingTtl
) {
}
