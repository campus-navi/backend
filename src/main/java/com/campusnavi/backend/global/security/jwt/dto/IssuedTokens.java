package com.campusnavi.backend.global.security.jwt.dto;

public record IssuedTokens(
        String accessToken,
        String refreshToken,
        String accessTokenJti,
        String refreshTokenJti
) {
}
