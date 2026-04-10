package com.campusnavi.backend.global.security.jwt;

public record RefreshTokenPayload(
        Long memberId,
        String jti
) {
}
