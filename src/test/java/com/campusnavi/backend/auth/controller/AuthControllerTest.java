package com.campusnavi.backend.auth.controller;

import com.campusnavi.backend.auth.dto.*;
import com.campusnavi.backend.auth.service.AuthService;
import com.campusnavi.backend.auth.service.EmailVerificationService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.util.cookie.RefreshTokenCookieProvider;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@ControllerSliceTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private RefreshTokenCookieProvider cookieProvider;

    private static final String EMAIL = "user@test.ac.kr";
    private static final String USERNAME = "testuser";
    private static final String NICKNAME = "testnick";
    private static final String PASSWORD = "Password1!";

    @Nested
    @DisplayName("이메일 인증코드 발송")
    class SendEmailVerification {

        private static final Long CAMPUS_ID = 1L;

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            EmailSendRequest request = new EmailSendRequest(CAMPUS_ID, EMAIL);

            mockMvc.perform(post("/api/v1/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("campusId가 null이면 400을 반환한다")
        void nullCampusId() throws Exception {
            EmailSendRequest request = new EmailSendRequest(null, EMAIL);

            mockMvc.perform(post("/api/v1/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("email이 없으면 400을 반환한다")
        void blankEmail() throws Exception {
            EmailSendRequest request = new EmailSendRequest(CAMPUS_ID, "");

            mockMvc.perform(post("/api/v1/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("email 형식이 아니면 400을 반환한다")
        void invalidEmailFormat() throws Exception {
            EmailSendRequest request = new EmailSendRequest(CAMPUS_ID, "not-email");

            mockMvc.perform(post("/api/v1/auth/email/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("이메일 인증코드 검증")
    class VerifyEmailCode {

        private static final String CODE = "123456";

        @Test
        @DisplayName("유효한 요청이면 200과 verifiedToken을 반환한다")
        void success() throws Exception {
            EmailVerifyRequest request = new EmailVerifyRequest(EMAIL, CODE);
            given(emailVerificationService.verifyEmailCode(any()))
                    .willReturn(new VerifiedTokenResponse("some-token"));

            mockMvc.perform(post("/api/v1/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.verifiedToken").value("some-token"));
        }

        @Test
        @DisplayName("email이 없으면 400을 반환한다")
        void blankEmail() throws Exception {
            EmailVerifyRequest request = new EmailVerifyRequest("", CODE);

            mockMvc.perform(post("/api/v1/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("email 형식이 아니면 400을 반환한다")
        void invalidEmailFormat() throws Exception {
            EmailVerifyRequest request = new EmailVerifyRequest("not-email", CODE);

            mockMvc.perform(post("/api/v1/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("code가 없으면 400을 반환한다")
        void blankCode() throws Exception {
            EmailVerifyRequest request = new EmailVerifyRequest(EMAIL, "");

            mockMvc.perform(post("/api/v1/auth/email/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        private static final String VERIFIED_TOKEN = "token-uuid";
        private static final Long DEPT_ID = 1L;
        private static final Integer ADMISSION_YEAR = 2026;

        @Test
        @DisplayName("유효한 요청이면 201과 Authorization, Set-Cookie 헤더를 반환한다")
        void success() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(authService.signUp(any())).willReturn(new TokenResponse("access-token", "refresh-token"));
            given(cookieProvider.setRefreshTokenCookie("refresh-token"))
                    .willReturn(ResponseCookie.from("refreshToken", "refresh-token").httpOnly(true).path("/").build());

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Authorization", "access-token"))
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("verifiedToken이 없으면 400을 반환한다")
        void blankVerifiedToken() throws Exception {
            SignUpRequest request = new SignUpRequest("", USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("username이 최소 길이 미만이면 400을 반환한다")
        void usernameTooShort() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, "ab", PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("username에 허용되지 않는 문자가 있으면 400을 반환한다")
        void usernameInvalidPattern() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, "test-user!", PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("password가 최소 길이 미만이면 400을 반환한다")
        void passwordTooShort() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, "Pass1!", NICKNAME, DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("password에 허용되지 않는 문자가 있으면 400을 반환한다")
        void passwordInvalidPattern() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, "한글패스워드!!!", NICKNAME, DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("nickname이 없으면 400을 반환한다")
        void blankNickname() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, "", DEPT_ID, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("departmentId가 null이면 400을 반환한다")
        void nullDepartmentId() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, null, ADMISSION_YEAR);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("admissionYear가 null이면 400을 반환한다")
        void nullAdmissionYear() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, null);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("이메일 인증이 안 된 토큰이면 403을 반환한다")
        void emailNotVerified() throws Exception {
            SignUpRequest request = new SignUpRequest("invalid-token", USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(authService.signUp(any())).willThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_NOT_VERIFIED.name()));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 409를 반환한다")
        void duplicateEmail() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, DEPT_ID, ADMISSION_YEAR);
            given(authService.signUp(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.name()));
        }

        @Test
        @DisplayName("존재하지 않는 학과이면 404를 반환한다")
        void departmentNotFound() throws Exception {
            SignUpRequest request = new SignUpRequest(VERIFIED_TOKEN, USERNAME, PASSWORD, NICKNAME, 999L, ADMISSION_YEAR);
            given(authService.signUp(any())).willThrow(new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND));

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DEPARTMENT_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("중복 검사")
    class DuplicateCheck {

        @Test
        @DisplayName("사용 가능한 username이면 200을 반환한다")
        void username_success() throws Exception {
            willDoNothing().given(authService).checkDuplicateUsername(USERNAME);

            mockMvc.perform(get("/api/v1/auth/check-username")
                            .param("username", USERNAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("이미 존재하는 username이면 409를 반환한다")
        void username_duplicate() throws Exception {
            willThrow(new BusinessException(ErrorCode.DUPLICATE_USERNAME)).given(authService).checkDuplicateUsername(USERNAME);

            mockMvc.perform(get("/api/v1/auth/check-username")
                            .param("username", USERNAME))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_USERNAME.name()));
        }

        @Test
        @DisplayName("사용 가능한 nickname이면 200을 반환한다")
        void nickname_success() throws Exception {
            willDoNothing().given(authService).checkDuplicateNickname(NICKNAME);

            mockMvc.perform(get("/api/v1/auth/check-nickname")
                            .param("nickname", NICKNAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("이미 존재하는 nickname이면 409를 반환한다")
        void nickname_duplicate() throws Exception {
            willThrow(new BusinessException(ErrorCode.DUPLICATE_NICKNAME)).given(authService).checkDuplicateNickname(NICKNAME);

            mockMvc.perform(get("/api/v1/auth/check-nickname")
                            .param("nickname", NICKNAME))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_NICKNAME.name()));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("유효한 refreshToken 쿠키가 있으면 200과 Authorization, Set-Cookie 헤더를 반환한다")
        void success() throws Exception {
            given(authService.reissue("valid-refresh-token"))
                    .willReturn(new TokenResponse("new-access-token", "new-refresh-token"));
            given(cookieProvider.setRefreshTokenCookie("new-refresh-token"))
                    .willReturn(ResponseCookie.from("refreshToken", "new-refresh-token").httpOnly(true).path("/").build());

            mockMvc.perform(post("/api/v1/auth/reissue")
                            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Authorization", "new-access-token"))
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=new-refresh-token")))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("refreshToken 쿠키가 없거나 유효하지 않으면 401을 반환한다")
        void invalidRefreshToken() throws Exception {
            given(authService.reissue(null))
                    .willThrow(new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

            mockMvc.perform(post("/api/v1/auth/reissue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_REFRESH_TOKEN.name()));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("유효한 토큰이 있으면 200과 만료된 Set-Cookie 헤더를 반환한다")
        void success() throws Exception {
            given(cookieProvider.expireRefreshTokenCookie())
                    .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).path("/").build());

            mockMvc.perform(post("/api/v1/auth/logout")
                            .header("Authorization", "Bearer valid-access-token")
                            .cookie(new Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("토큰이 없어도 200을 반환한다")
        void noTokens() throws Exception {
            given(cookieProvider.expireRefreshTokenCookie())
                    .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).path("/").build());

            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("유효한 요청이면 200과 Authorization, Set-Cookie 헤더를 반환한다")
        void success() throws Exception {
            LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
            given(authService.login(any())).willReturn(new TokenResponse("access-token", "refresh-token"));
            given(cookieProvider.setRefreshTokenCookie("refresh-token"))
                    .willReturn(ResponseCookie.from("refreshToken", "refresh-token").httpOnly(true).path("/").build());

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Authorization", "access-token"))
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=refresh-token")))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("username이 없으면 400을 반환한다")
        void blankUsername() throws Exception {
            LoginRequest request = new LoginRequest("", PASSWORD);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("password가 없으면 400을 반환한다")
        void blankPassword() throws Exception {
            LoginRequest request = new LoginRequest(USERNAME, "");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("username을 찾을 수 없거나 password가 일치하지 않으면 401을 반환한다")
        void invalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest(USERNAME, "wrongpass");
            given(authService.login(any())).willThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.name()));
        }
    }
}
