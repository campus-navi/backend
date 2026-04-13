package com.campusnavi.backend.auth.service;

import com.campusnavi.backend.auth.dto.SignUpRequest;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.infra.redis.RedisKeys;
import com.campusnavi.backend.infra.redis.RedisService;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

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

    @InjectMocks
    private AuthService authService;

    private static final String USERNAME = "testuser";
    private static final String NICKNAME = "testnick";

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
        private static final String PASSWORD = "Password1!";
        private static final Long DEPT_ID = 1L;

        @Test
        @DisplayName("정상 요청이면 회원을 저장하고 verifiedToken을 Redis에서 삭제한다")
        void success() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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

            // when
            authService.signUp(request);

            // then
            then(memberRepository).should().save(any());
            then(redisService).should().delete(RedisKeys.emailVerified(VERIFIED_TOKEN));
        }

        @Test
        @DisplayName("verifiedToken이 Redis에 없으면 EMAIL_NOT_VERIFIED 예외가 발생한다")
        void verifiedTokenNotFound() {
            // given
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, 2024);
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
}
