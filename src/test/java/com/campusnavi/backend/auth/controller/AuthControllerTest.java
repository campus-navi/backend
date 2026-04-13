package com.campusnavi.backend.auth.controller;

import com.campusnavi.backend.auth.dto.SignUpRequest;
import com.campusnavi.backend.auth.service.AuthService;
import com.campusnavi.backend.auth.service.EmailVerificationService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ControllerSliceTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    private static final String USERNAME = "testuser";
    private static final String NICKNAME = "testnick";

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("유효한 요청이면 200을 반환한다")
        void success() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", USERNAME, "Password1!", NICKNAME, 1L, 2024);
            willDoNothing().given(authService).signUp(request);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("verifiedToken이 없으면 400을 반환한다")
        void blankVerifiedToken() throws Exception {
            SignUpRequest request = new SignUpRequest("", USERNAME, "Password1!", NICKNAME, 1L, 2024);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
        }

        @Test
        @DisplayName("username이 최소 길이 미만이면 400을 반환한다")
        void usernameTooShort() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", "ab", "Password1!", NICKNAME, 1L, 2024);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
        }

        @Test
        @DisplayName("username에 허용되지 않는 문자가 있으면 400을 반환한다")
        void usernameInvalidPattern() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", "test-user!", "Password1!", NICKNAME, 1L, 2024);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
        }

        @Test
        @DisplayName("departmentId가 null이면 400을 반환한다")
        void nullDepartmentId() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", USERNAME, "Password1!", NICKNAME, null, 2024);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.getCode()));
        }

        @Test
        @DisplayName("이메일 인증이 안 된 토큰이면 403을 반환한다")
        void emailNotVerified() throws Exception {
            SignUpRequest request = new SignUpRequest("invalid-token", USERNAME, "Password1!", NICKNAME, 1L, 2024);
            willThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED)).given(authService).signUp(request);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_NOT_VERIFIED.getCode()));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 409를 반환한다")
        void duplicateEmail() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", USERNAME, "Password1!", NICKNAME, 1L, 2024);
            willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL)).given(authService).signUp(request);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_EMAIL.getCode()));
        }

        @Test
        @DisplayName("존재하지 않는 학과이면 404를 반환한다")
        void departmentNotFound() throws Exception {
            SignUpRequest request = new SignUpRequest("token-uuid", USERNAME, "Password1!", NICKNAME, 999L, 2024);
            willThrow(new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND)).given(authService).signUp(request);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DEPARTMENT_NOT_FOUND.getCode()));
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
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_USERNAME.getCode()));
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
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_NICKNAME.getCode()));
        }
    }
}
