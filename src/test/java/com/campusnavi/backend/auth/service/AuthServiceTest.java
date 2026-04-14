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
import com.campusnavi.backend.member.entity.MemberRole;
import com.campusnavi.backend.member.repository.MemberRepository;
import com.campusnavi.backend.university.entity.Campus;
import com.campusnavi.backend.university.entity.Department;
import com.campusnavi.backend.university.entity.University;
import com.campusnavi.backend.university.repository.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private static final String USERNAME = "testuser";
    private static final String NICKNAME = "testnick";
    private static final String PASSWORD = "Password1!";

    @Nested
    @DisplayName("중복 검사")
    class DuplicateCheck {

        @Test
        @DisplayName("사용 가능한 username이면 예외가 발생하지 않는다")
        void username_notDuplicated() {
            // given
            given(memberRepository.existsByUsername(USERNAME)).willReturn(false);

            // when & then
            assertThatCode(() -> authService.checkDuplicateUsername(USERNAME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 존재하는 username이면 DUPLICATE_USERNAME 예외가 발생한다")
        void username_duplicated() {
            // given
            given(memberRepository.existsByUsername(USERNAME)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.checkDuplicateUsername(USERNAME))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME));
        }

        @Test
        @DisplayName("사용 가능한 nickname이면 예외가 발생하지 않는다")
        void nickname_notDuplicated() {
            // given
            given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);

            // when & then
            assertThatCode(() -> authService.checkDuplicateNickname(NICKNAME))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("이미 존재하는 nickname이면 DUPLICATE_NICKNAME 예외가 발생한다")
        void nickname_duplicated() {
            // given
            given(memberRepository.existsByNickname(NICKNAME)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.checkDuplicateNickname(NICKNAME))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
        }
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        private static final String EMAIL = "example@test.ac.kr";
        private static final String VERIFIED_TOKEN = "test-verified-uuid";
        private static final Long DEPT_ID = 1L;
        private static final Integer ADMISSION_YEAR = 2026;

        @Test
        @DisplayName("정상 요청이면 회원을 저장하고 TokenResponse를 반환한다")
        void success() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            University university = mock(University.class);
            Campus campus = mock(Campus.class);
            Department department = mock(Department.class);

            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(EMAIL);
            given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
            given(memberRepository.existsByUsername(USERNAME)).willReturn(false);
            given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);
            given(departmentRepository.findByIdWithCampusAndUniversity(DEPT_ID)).willReturn(Optional.of(department));
            given(department.getCampus()).willReturn(campus);
            given(campus.getUniversity()).willReturn(university);
            given(university.getId()).willReturn(1L);
            given(passwordEncoder.encode(PASSWORD)).willReturn("encoded-password");
            given(jwtProvider.issueTokens(any(), any())).willReturn(
                    new IssuedTokens("access-token", "refresh-token", "access-jti", "refresh-jti"));

            // when
            TokenResponse result = authService.signUp(request);

            // then
            then(memberRepository).should().save(any());
            then(redisService).should().delete(RedisKeys.emailVerified(VERIFIED_TOKEN));
            then(redisService).should().set(eq(RedisKeys.refreshToken("refresh-jti")), eq("refresh-token"), any());
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("verifiedToken이 Redis에 없으면 EMAIL_NOT_VERIFIED 예외가 발생한다")
        void verifiedTokenNotFound() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 DUPLICATE_EMAIL 예외가 발생한다")
        void duplicateEmail() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(EMAIL);
            given(memberRepository.existsByEmail(EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        }

        @Test
        @DisplayName("이미 존재하는 username이면 DUPLICATE_USERNAME 예외가 발생한다")
        void duplicateUsername() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(EMAIL);
            given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
            given(memberRepository.existsByUsername(USERNAME)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_USERNAME));
        }

        @Test
        @DisplayName("이미 존재하는 nickname이면 DUPLICATE_NICKNAME 예외가 발생한다")
        void duplicateNickname() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(EMAIL);
            given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
            given(memberRepository.existsByUsername(USERNAME)).willReturn(false);
            given(memberRepository.existsByNickname(NICKNAME)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
        }

        @Test
        @DisplayName("존재하지 않는 departmentId면 DEPARTMENT_NOT_FOUND 예외가 발생한다")
        void departmentNotFound() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(redisService.get(RedisKeys.emailVerified(VERIFIED_TOKEN))).willReturn(EMAIL);
            given(memberRepository.existsByEmail(EMAIL)).willReturn(false);
            given(memberRepository.existsByUsername(USERNAME)).willReturn(false);
            given(memberRepository.existsByNickname(NICKNAME)).willReturn(false);
            given(departmentRepository.findByIdWithCampusAndUniversity(DEPT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.signUp(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        private static final String REFRESH_TOKEN = "valid-refresh-token";
        private static final String REFRESH_JTI = "refresh-jti";
        private static final Long MEMBER_ID = 1L;

        @Test
        @DisplayName("정상 요청이면 기존 토큰을 삭제하고 새 TokenResponse를 반환한다")
        void success() {
            // given
            RefreshTokenPayload payload = new RefreshTokenPayload(MEMBER_ID, REFRESH_JTI);
            Member member = mock(Member.class);

            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(payload);
            given(redisService.get(RedisKeys.refreshToken(REFRESH_JTI))).willReturn(REFRESH_TOKEN);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(MEMBER_ID);
            given(member.getRole()).willReturn(MemberRole.USER);
            given(jwtProvider.issueTokens(MEMBER_ID, MemberRole.USER)).willReturn(
                    new IssuedTokens("new-access-token", "new-refresh-token", "new-access-jti", "new-refresh-jti"));

            // when
            TokenResponse result = authService.reissue(REFRESH_TOKEN);

            // then
            then(redisService).should().delete(RedisKeys.refreshToken(REFRESH_JTI));
            then(redisService).should().set(eq(RedisKeys.refreshToken("new-refresh-jti")), eq("new-refresh-token"), any());
            assertThat(result.accessToken()).isEqualTo("new-access-token");
            assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        }

        @Test
        @DisplayName("refreshToken이 null이면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void nullRefreshToken() {
            // when & then
            assertThatThrownBy(() -> authService.reissue(null))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("Redis에 저장된 토큰이 없으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void storedTokenNotFound() {
            // given
            RefreshTokenPayload payload = new RefreshTokenPayload(MEMBER_ID, REFRESH_JTI);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(payload);
            given(redisService.get(RedisKeys.refreshToken(REFRESH_JTI))).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("Redis에 저장된 토큰과 일치하지 않으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void storedTokenMismatch() {
            // given
            RefreshTokenPayload payload = new RefreshTokenPayload(MEMBER_ID, REFRESH_JTI);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(payload);
            given(redisService.get(RedisKeys.refreshToken(REFRESH_JTI))).willReturn("different-token");

            // when & then
            assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 INVALID_REFRESH_TOKEN 예외가 발생한다")
        void memberNotFound() {
            // given
            RefreshTokenPayload payload = new RefreshTokenPayload(MEMBER_ID, REFRESH_JTI);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(payload);
            given(redisService.get(RedisKeys.refreshToken(REFRESH_JTI))).willReturn(REFRESH_TOKEN);
            given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissue(REFRESH_TOKEN))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        private static final String ACCESS_TOKEN = "Bearer valid-access-token";
        private static final String REFRESH_TOKEN = "valid-refresh-token";
        private static final String ACCESS_JTI = "access-jti";
        private static final String REFRESH_JTI = "refresh-jti";
        private static final long REMAINING_TTL = 3_600_000L;

        @Test
        @DisplayName("유효한 토큰이면 accessToken을 블랙리스트에 등록하고 refreshToken을 삭제한다")
        void success() {
            // given
            AccessTokenPayload accessPayload = new AccessTokenPayload(1L, "USER", ACCESS_JTI, REMAINING_TTL);
            RefreshTokenPayload refreshPayload = new RefreshTokenPayload(1L, REFRESH_JTI);

            given(jwtProvider.parseAndValidateAccessToken("valid-access-token")).willReturn(accessPayload);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(refreshPayload);

            // when
            assertThatCode(() -> authService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                    .doesNotThrowAnyException();

            // then
            then(redisService).should().set(RedisKeys.blacklist(ACCESS_JTI), "logout", Duration.ofMillis(REMAINING_TTL));
            then(redisService).should().delete(RedisKeys.refreshToken(REFRESH_JTI));
        }

        @Test
        @DisplayName("accessToken이 null이면 블랙리스트에 등록하지 않는다")
        void nullAccessToken() {
            // given
            RefreshTokenPayload refreshPayload = new RefreshTokenPayload(1L, REFRESH_JTI);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(refreshPayload);

            // when
            assertThatCode(() -> authService.logout(null, REFRESH_TOKEN))
                    .doesNotThrowAnyException();

            // then
            then(redisService).should(never()).set(any(), any(), any());
        }

        @Test
        @DisplayName("accessToken에 Bearer 접두사가 없으면 블랙리스트에 등록하지 않는다")
        void accessTokenWithoutBearerPrefix() {
            // given
            RefreshTokenPayload refreshPayload = new RefreshTokenPayload(1L, REFRESH_JTI);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(refreshPayload);

            // when
            assertThatCode(() -> authService.logout("no-prefix-token", REFRESH_TOKEN))
                    .doesNotThrowAnyException();

            // then
            then(redisService).should(never()).set(any(), any(), any());
        }

        @Test
        @DisplayName("accessToken의 remainingTtl이 0 이하이면 블랙리스트에 등록하지 않는다")
        void accessTokenExpired() {
            // given
            AccessTokenPayload accessPayload = new AccessTokenPayload(1L, "USER", ACCESS_JTI, 0L);
            RefreshTokenPayload refreshPayload = new RefreshTokenPayload(1L, REFRESH_JTI);

            given(jwtProvider.parseAndValidateAccessToken("valid-access-token")).willReturn(accessPayload);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(refreshPayload);

            // when
            assertThatCode(() -> authService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                    .doesNotThrowAnyException();

            // then
            then(redisService).should(never()).set(any(), any(), any());
        }

        @Test
        @DisplayName("accessToken 파싱 중 JwtAuthenticationException이 발생해도 예외 없이 처리된다")
        void accessTokenParseException() {
            // given
            RefreshTokenPayload refreshPayload = new RefreshTokenPayload(1L, REFRESH_JTI);

            given(jwtProvider.parseAndValidateAccessToken("valid-access-token"))
                    .willThrow(new JwtAuthenticationException(ErrorCode.INVALID_TOKEN));
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN)).willReturn(refreshPayload);

            // when & then
            assertThatCode(() -> authService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                    .doesNotThrowAnyException();
            then(redisService).should(never()).set(any(), any(), any());
        }

        @Test
        @DisplayName("refreshToken이 null이면 refreshToken을 삭제하지 않는다")
        void nullRefreshToken() {
            // given
            AccessTokenPayload accessPayload = new AccessTokenPayload(1L, "USER", ACCESS_JTI, REMAINING_TTL);
            given(jwtProvider.parseAndValidateAccessToken("valid-access-token")).willReturn(accessPayload);

            // when
            assertThatCode(() -> authService.logout(ACCESS_TOKEN, null))
                    .doesNotThrowAnyException();

            // then
            then(redisService).should(never()).delete(any());
        }

        @Test
        @DisplayName("refreshToken 파싱 중 JwtAuthenticationException이 발생해도 예외 없이 처리된다")
        void refreshTokenParseException() {
            // given
            AccessTokenPayload accessPayload = new AccessTokenPayload(1L, "USER", ACCESS_JTI, REMAINING_TTL);
            given(jwtProvider.parseAndValidateAccessToken("valid-access-token")).willReturn(accessPayload);
            given(jwtProvider.parseAndValidateRefreshToken(REFRESH_TOKEN))
                    .willThrow(new JwtAuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN));

            // when & then
            assertThatCode(() -> authService.logout(ACCESS_TOKEN, REFRESH_TOKEN))
                    .doesNotThrowAnyException();
            then(redisService).should(never()).delete(any());
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        private static final String ENCODED_PASSWORD = "encoded-password";

        @Test
        @DisplayName("정상 요청이면 accessToken과 refreshToken을 반환한다")
        void success() {
            // given
            LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
            Member member = mock(Member.class);

            given(memberRepository.findByUsername(USERNAME)).willReturn(Optional.of(member));
            given(member.getId()).willReturn(1L);
            given(member.getPassword()).willReturn(ENCODED_PASSWORD);
            given(member.getRole()).willReturn(MemberRole.USER);
            given(passwordEncoder.matches(PASSWORD, ENCODED_PASSWORD)).willReturn(true);
            given(jwtProvider.issueTokens(1L, MemberRole.USER)).willReturn(
                    new IssuedTokens("access-token", "refresh-token", "access-jti", "refresh-jti"));

            // when
            TokenResponse result = authService.login(request);

            // then
            then(redisService).should().set(eq(RedisKeys.refreshToken("refresh-jti")), eq("refresh-token"), any());
            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("존재하지 않는 username이면 INVALID_CREDENTIALS 예외가 발생한다")
        void usernameNotFound() {
            // given
            LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
            given(memberRepository.findByUsername(USERNAME)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 INVALID_CREDENTIALS 예외가 발생한다")
        void invalidPassword() {
            // given
            LoginRequest request = new LoginRequest(USERNAME, "wrongpassword");
            Member member = mock(Member.class);

            given(memberRepository.findByUsername(USERNAME)).willReturn(Optional.of(member));
            given(member.getPassword()).willReturn(ENCODED_PASSWORD);
            given(passwordEncoder.matches("wrongpassword", ENCODED_PASSWORD)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOfSatisfying(BusinessException.class, e ->
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        }
    }
}
