package com.campusnavi.backend.global.security.jwt;

import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.exception.JwtAuthenticationException;
import com.campusnavi.backend.member.entity.MemberRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtProviderTest {

    private static final String TEST_SECRET = "this-is-a-test-secret-key-for-jwt-provider-test-at-least-32characters";

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                TEST_SECRET,
                Duration.ofHours(1),
                Duration.ofDays(7)
        );
        jwtProvider = new JwtProvider(properties);
    }

    @Test
    @DisplayName("AccessToken 생성 후 파싱하면 memberId와 role, jti를 추출할 수 있다.")
    void generateAccessToken_thenParseReturnsCorrectPayload() {
        // given
        Long memberId = 1L;
        MemberRole role = MemberRole.USER;

        // when
        String token = jwtProvider.generateAccessToken(memberId, role);
        AccessTokenPayload payload = jwtProvider.parseAndValidateAccessToken(token);

        // then
        assertThat(payload.memberId()).isEqualTo(memberId);
        assertThat(payload.role()).isEqualTo(role.name());
        assertThat(payload.jti()).isNotBlank();
    }

    @Test
    @DisplayName("RefreshToken 생성 후 파싱하면 memberId와 jti를 추출할 수 있다.")
    void generateRefreshToken_thenParseReturnsCorrectPayload() {
        // given
        Long memberId = 2L;

        // when
        String token = jwtProvider.generateRefreshToken(memberId);
        RefreshTokenPayload payload = jwtProvider.parseAndValidateRefreshToken(token);

        // then
        assertThat(payload.memberId()).isEqualTo(memberId);
        assertThat(payload.jti()).isNotBlank();
    }

    @Test
    @DisplayName("RefreshToken을 AccessToken으로 파싱하면 INVALID_TOKEN_TYPE 예외가 발생한다")
    void parseAccessToken_withRefreshToken_throwsInvalidTokenType() {
        // given
        String refreshToken = jwtProvider.generateRefreshToken(1L);

        // when, then
        assertThatThrownBy(() -> jwtProvider.parseAndValidateAccessToken(refreshToken))
                .isInstanceOfSatisfying(JwtAuthenticationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN_TYPE);
                });
    }

    @Test
    @DisplayName("AccessToken을 RefreshToken으로 파싱하면 INVALID_TOKEN_TYPE 예외가 발생한다")
    void parseRefreshToken_withAccessToken_throwsInvalidTokenType() {
        // given
        String accessToken = jwtProvider.generateAccessToken(1L, MemberRole.USER);

        // when, then
        assertThatThrownBy(() -> jwtProvider.parseAndValidateRefreshToken(accessToken))
                .isInstanceOfSatisfying(JwtAuthenticationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN_TYPE);
                });
    }

    @Test
    @DisplayName("만료된 토큰을 파싱하면 EXPIRED_TOKEN 예외가 발생한다")
    void parseExpiredToken_throwsExpiredToken() {
        // given
        JwtProperties expired = new JwtProperties(
                TEST_SECRET,
                Duration.ofMillis(-1),
                Duration.ofDays(7)
        );
        JwtProvider expiredJwtProvider = new JwtProvider(expired);
        String expiredToken = expiredJwtProvider.generateAccessToken(1L, MemberRole.USER);

        // when, then
        assertThatThrownBy(() -> jwtProvider.parseAndValidateAccessToken(expiredToken))
                .isInstanceOfSatisfying(JwtAuthenticationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN);
                });
    }

    @Test
    @DisplayName("잘못된 형식의 토큰을 파싱하면 INVALID_TOKEN 예외가 발생한다")
    void parseInvalidFormatToken_throwsInvalidToken() {
        // given
        String invalidToken = "잘못된 토큰";

        // when, then
        assertThatThrownBy(() -> jwtProvider.parseAndValidateAccessToken(invalidToken))
                .isInstanceOfSatisfying(JwtAuthenticationException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
                });
    }

    @Test
    @DisplayName("유효한 AccessToken의 TTL은 만료시간 이내이며 양수이다")
    void getRemainingTtl_withValidToken_returnsPositiveValue() {
        // given
        String token = jwtProvider.generateAccessToken(1L, MemberRole.USER);

        // when
        long ttl = jwtProvider.getRemainingTtl(token);

        // then
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(Duration.ofHours(1).toMillis());
    }
}
