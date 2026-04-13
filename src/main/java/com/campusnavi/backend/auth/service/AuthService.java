package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.auth.dto.LoginRequest;
import com.campusnavi.backend.auth.dto.SignUpRequest;
import com.campusnavi.backend.auth.dto.TokenResponse;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.jwt.JwtProperties;
import com.campusnavi.backend.global.security.jwt.JwtProvider;
import com.campusnavi.backend.global.security.jwt.RefreshTokenPayload;
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

        //회원가입시 즉시 로그인
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());

        RefreshTokenPayload payload = jwtProvider.parseAndValidateRefreshToken(refreshToken);
        redisService.set(RedisKeys.refreshToken(payload.jti()), refreshToken, jwtProperties.refreshTokenExpiration());

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        Long memberId = member.getId();
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.generateAccessToken(memberId, member.getRole());
        String refreshToken = jwtProvider.generateRefreshToken(memberId);

        RefreshTokenPayload payload = jwtProvider.parseAndValidateRefreshToken(refreshToken);
        redisService.set(RedisKeys.refreshToken(payload.jti()), refreshToken, jwtProperties.refreshTokenExpiration());

        return new TokenResponse(accessToken, refreshToken);
    }
}
