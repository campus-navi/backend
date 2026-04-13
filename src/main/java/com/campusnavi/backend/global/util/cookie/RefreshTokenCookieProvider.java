package com.campusnavi.backend.global.util.cookie;

import com.campusnavi.backend.global.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieProvider {

    private static final String REFRESH_TOKEN = "refreshToken";
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    public ResponseCookie setRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(jwtProperties.refreshTokenExpiration())
                .path("/")
                .build();

    }

    public ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite(cookieProperties.sameSite())
                .maxAge(0)
                .path("/")
                .build();
    }
}
