package com.campusnavi.backend.member.controller;

import com.campusnavi.backend.auth.service.AuthService;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.global.util.cookie.RefreshTokenCookieProvider;
import com.campusnavi.backend.member.dto.AdmissionYearUpdateRequest;
import com.campusnavi.backend.member.dto.GradeUpdateRequest;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.dto.PasswordUpdateRequest;
import com.campusnavi.backend.member.dto.UsernameUpdateRequest;
import com.campusnavi.backend.member.service.MemberService;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieProvider cookieProvider;

    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(1L, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("관심사 전체 교체")
    class UpdateInterests {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(List.of(1L, 2L));
            willDoNothing().given(memberService).updateMemberInterests(any(), any());

            // when & then
            mockMvc.perform(put("/api/v1/members/me/interests")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("빈 배열을 전달해도 200을 반환한다")
        void emptyList() throws Exception {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(List.of());
            willDoNothing().given(memberService).updateMemberInterests(any(), any());

            // when & then
            mockMvc.perform(put("/api/v1/members/me/interests")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("interestIds가 null이면 400을 반환한다")
        void nullInterestIds() throws Exception {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(null);

            // when & then
            mockMvc.perform(put("/api/v1/members/me/interests")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("존재하지 않는 tagId가 포함되면 404를 반환한다")
        void tagNotFound() throws Exception {
            // given
            MemberInterestUpdateRequest request = new MemberInterestUpdateRequest(List.of(1L, 999L));
            willThrow(new BusinessException(ErrorCode.TAG_NOT_FOUND))
                    .given(memberService).updateMemberInterests(any(), any());

            // when & then
            mockMvc.perform(put("/api/v1/members/me/interests")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.TAG_NOT_FOUND.name()));
        }
    }

    @Nested
    @DisplayName("아이디 변경")
    class ChangeUsername {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            // given
            UsernameUpdateRequest request = new UsernameUpdateRequest("newname1");
            willDoNothing().given(memberService).changeUsername(any(), any());

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/username")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("형식에 맞지 않는 아이디면 400을 반환한다")
        void invalidUsername() throws Exception {
            // given
            UsernameUpdateRequest request = new UsernameUpdateRequest("AB");

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/username")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }

        @Test
        @DisplayName("이미 사용 중인 아이디면 409를 반환한다")
        void duplicateUsername() throws Exception {
            // given
            UsernameUpdateRequest request = new UsernameUpdateRequest("dupname1");
            willThrow(new BusinessException(ErrorCode.DUPLICATE_USERNAME))
                    .given(memberService).changeUsername(any(), any());

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/username")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_USERNAME.name()));
        }
    }

    @Nested
    @DisplayName("비밀번호 변경")
    class ChangePassword {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("newpass1!");
            willDoNothing().given(memberService).changePassword(any(), any());

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/password")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("형식에 맞지 않는 비밀번호면 400을 반환한다")
        void invalidPassword() throws Exception {
            // given
            PasswordUpdateRequest request = new PasswordUpdateRequest("short");

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/password")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("학번 변경")
    class ChangeAdmissionYear {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            // given
            AdmissionYearUpdateRequest request = new AdmissionYearUpdateRequest(25);
            willDoNothing().given(memberService).changeAdmissionYear(any(), any());

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/admission-year")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("학번이 null이면 400을 반환한다")
        void nullAdmissionYear() throws Exception {
            // given
            AdmissionYearUpdateRequest request = new AdmissionYearUpdateRequest(null);

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/admission-year")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("학년 변경")
    class ChangeGrade {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            // given
            GradeUpdateRequest request = new GradeUpdateRequest(2);
            willDoNothing().given(memberService).changeGrade(any(), any());

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/grade")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("학년이 범위를 벗어나면 400을 반환한다")
        void invalidGrade() throws Exception {
            // given
            GradeUpdateRequest request = new GradeUpdateRequest(5);

            // when & then
            mockMvc.perform(patch("/api/v1/members/me/grade")
                            .with(authentication(AUTH))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT.name()));
        }
    }

    @Nested
    @DisplayName("회원탈퇴")
    class Withdraw {

        @Test
        @DisplayName("정상 요청이면 204와 만료된 Set-Cookie를 반환한다")
        void success() throws Exception {
            // given
            willDoNothing().given(memberService).withdraw(anyLong());
            given(cookieProvider.expireRefreshTokenCookie())
                    .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).path("/").build());

            // when & then
            mockMvc.perform(delete("/api/v1/members/me")
                            .with(authentication(AUTH))
                            .header("Authorization", "Bearer valid-access-token")
                            .cookie(new jakarta.servlet.http.Cookie("refreshToken", "valid-refresh-token")))
                    .andExpect(status().isNoContent())
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));
            then(authService).should().logout("Bearer valid-access-token", "valid-refresh-token");
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 404를 반환한다")
        void memberNotFound() throws Exception {
            // given
            willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                    .given(memberService).withdraw(anyLong());

            // when & then
            mockMvc.perform(delete("/api/v1/members/me")
                            .with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.name()));
            then(authService).should(never()).logout(any(), any());
        }
    }
}
