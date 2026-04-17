package com.campusnavi.backend.global.security.jwt.dto;

public record RefreshTokenPayload(
        Long memberId,
        String jti
) {
}
