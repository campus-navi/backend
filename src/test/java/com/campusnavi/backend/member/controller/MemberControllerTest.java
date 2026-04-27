package com.campusnavi.backend.member.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.member.dto.MemberInterestUpdateRequest;
import com.campusnavi.backend.member.service.MemberService;
import com.campusnavi.backend.support.ControllerSliceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MemberService memberService;

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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.TAG_NOT_FOUND.name()));
        }
    }
}
