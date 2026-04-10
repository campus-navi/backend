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
import java.util.Objects;
import java.util.UUID;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;
    private static final String ROLE = "role";
    private static final String TYPE = "type";

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
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

    public AccessTokenPayload parseAndValidateAccessToken(String token) {
        Claims claims = parseToken(token);
        if (!Objects.equals(claims.get(TYPE, String.class), TokenType.ACCESS.name())) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN_TYPE);
        }
        Long memberId = Long.parseLong(claims.getSubject());
        MemberRole role = MemberRole.valueOf(claims.get(ROLE, String.class));
        String jti = claims.getId();
        return new AccessTokenPayload(memberId,role.name(),jti);
    }

    public RefreshTokenPayload parseAndValidateRefreshToken(String token) {
        Claims claims = parseToken(token);
        if (!Objects.equals(claims.get(TYPE, String.class), TokenType.REFRESH.name())) {
            throw new JwtAuthenticationException(ErrorCode.INVALID_TOKEN_TYPE);
        }
        Long memberId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();
        return new RefreshTokenPayload(memberId,jti);
    }

    public long getRemainingTtl(String token) {
        long remaining = parseToken(token).getExpiration().getTime();
        return remaining - System.currentTimeMillis();
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
