package com.campusnavi.backend.mypage.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.mypage.service.MyPageService;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = MyPageController.class)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyPageService myPageService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID), null, List.of()
    );

    @Test
    @DisplayName("정상 요청이면 200과 마이페이지 데이터를 반환한다")
    void success() throws Exception {
        given(myPageService.getMyPage(MEMBER_ID)).willReturn(new MyPageResponse(
                "testnick", "user@test.ac.kr", "테스트대학교(서울캠퍼스)", 25, 1,
                List.of("컴퓨터공학과"), 5L, 4L, 3L));

        mockMvc.perform(get("/api/v1/mypage").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("testnick"))
                .andExpect(jsonPath("$.data.email").value("user@test.ac.kr"))
                .andExpect(jsonPath("$.data.campus").value("테스트대학교(서울캠퍼스)"))
                .andExpect(jsonPath("$.data.admissionYear").value(25))
                .andExpect(jsonPath("$.data.grade").value(1))
                .andExpect(jsonPath("$.data.departments[0]").value("컴퓨터공학과"))
                .andExpect(jsonPath("$.data.scrapCount").value(5))
                .andExpect(jsonPath("$.data.remindCount").value(4))
                .andExpect(jsonPath("$.data.interestCount").value(3));
    }

    @Test
    @DisplayName("회원이 없으면 404와 MEMBER_NOT_FOUND를 반환한다")
    void memberNotFound() throws Exception {
        willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND))
                .given(myPageService).getMyPage(MEMBER_ID);

        mockMvc.perform(get("/api/v1/mypage").with(authentication(AUTH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }
}
