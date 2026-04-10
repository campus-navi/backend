package com.campusnavi.backend.global.security.jwt;

public record AccessTokenPayload(
        Long memberId,
        String role,
        String jti
) {
}
