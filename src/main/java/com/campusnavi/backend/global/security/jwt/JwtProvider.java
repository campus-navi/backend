package com.campusnavi.backend.global.security.jwt;

import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.exception.JwtAuthenticationException;
import com.campusnavi.backend.member.entity.MemberRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    private static final String ROLE = "role";
    private static final String TYPE = "type";

    @PostConstruct
    private void init() {
        this.secretKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(Long memberId, MemberRole role) {
        return buildBaseToken(memberId, TokenType.ACCESS, jwtProperties.accessTokenExpiration())
                .claim(ROLE, role.name())
                .compact();
    }

    public String generateRefreshToken(Long memberId) {
        return buildBaseToken(memberId, TokenType.REFRESH, jwtProperties.refreshTokenExpiration())
                .compact();
    }

    public Claims validateToken(String token, TokenType expectedType) {
        Claims claims = parseToken(token);
        validateTokenType(token, expectedType);
        return claims;
    }

    public Long extractMemberId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public MemberRole extractMemberRole(String token) {
        String role = parseToken(token).get(ROLE, String.class);
        if (role == null) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
        return MemberRole.valueOf(role);
    }

    public String extractJti(String token) {
        return parseToken(token).getId();
    }

    public long getRemainingTtl(String token) {
        long remaining = parseToken(token).getExpiration().getTime();
        return remaining - System.currentTimeMillis();
    }

    private void validateTokenType(String token, TokenType expectedType) {
       String actualType = parseToken(token).get(TYPE, String.class);
       if (!expectedType.name().equals(actualType)) {
           throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN_TYPE);
       }
    }

    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN);
        }
    }

    private JwtBuilder buildBaseToken(Long memberId, TokenType type, Duration expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(memberId.toString())
                .issuedAt(now)
                .expiration(expiry)
                .claim(TYPE, type.name())
                .signWith(secretKey);
    }
}
