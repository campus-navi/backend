package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.auth.dto.LoginRequest;
import com.campusnavi.backend.auth.dto.SignUpRequest;
import com.campusnavi.backend.auth.dto.TokenResponse;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.exception.JwtAuthenticationException;
import com.campusnavi.backend.global.security.jwt.JwtProperties;
import com.campusnavi.backend.global.security.jwt.JwtProvider;
import com.campusnavi.backend.global.security.jwt.dto.AccessTokenPayload;
import com.campusnavi.backend.global.security.jwt.dto.IssuedTokens;
import com.campusnavi.backend.global.security.jwt.dto.RefreshTokenPayload;
import com.campusnavi.backend.infra.redis.RedisKeys;
import com.campusnavi.backend.infra.redis.RedisService;
import com.campusnavi.backend.member.entity.Member;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public void checkDuplicateUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    public void checkDuplicateNickname(String nickname) {
        if (memberRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
    }

    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        String email = redisService.get(RedisKeys.emailVerified(request.verifiedToken()));
        if (email == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        checkDuplicateUsername(request.username());
        checkDuplicateNickname(request.nickname());

        Department department = departmentRepository.findByIdWithCampusAndUniversity(request.departmentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));
        Campus campus = department.getCampus();
        Long universityId = campus.getUniversity().getId();

        String encodedPassword = passwordEncoder.encode(request.password());
        Member member = Member.join(email, request.username(), encodedPassword, request.nickname(),
                universityId, campus, request.admissionYear());
        member.addDepartment(department);
        memberRepository.save(member);

        redisService.delete(RedisKeys.emailVerified(request.verifiedToken()));

        return issueTokens(member);
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(member);
    }

    public TokenResponse reissue(String refreshToken) {
        if (refreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshTokenPayload payload = jwtProvider.parseAndValidateRefreshToken(refreshToken);

        String storedToken = redisService.get(RedisKeys.refreshToken(payload.jti()));

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        redisService.delete(RedisKeys.refreshToken(payload.jti()));

        return issueTokens(member);
    }

    public void logout(String accessToken, String refreshToken) {

        blacklistAccessToken(accessToken);
        deleteRefreshToken(refreshToken);
    }

    private TokenResponse issueTokens(Member member) {
        IssuedTokens tokens = jwtProvider.issueTokens(member.getId(), member.getRole());
        redisService.set(RedisKeys.refreshToken(tokens.refreshTokenJti()), tokens.refreshToken(), jwtProperties.refreshTokenExpiration());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    private void blacklistAccessToken(String accessToken){
        if (accessToken == null || accessToken.isBlank() || !accessToken.startsWith("Bearer ")) {
            return;
        }
        try {
            accessToken = accessToken.substring(7);
            AccessTokenPayload payload = jwtProvider.parseAndValidateAccessToken(accessToken);
            if (payload.remainingTtl() <= 0){
                return;
            }
            redisService.set(RedisKeys.blacklist(payload.jti()), "logout", Duration.ofMillis(payload.remainingTtl()));
        } catch (JwtAuthenticationException ignored){
        }
    }

    private void deleteRefreshToken(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            RefreshTokenPayload payload = jwtProvider.parseAndValidateRefreshToken(refreshToken);
            redisService.delete(RedisKeys.refreshToken(payload.jti()));
        } catch (JwtAuthenticationException ignored){
        }
    }
}
