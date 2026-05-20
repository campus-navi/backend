package com.campusnavi.backend.mypage.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.mypage.dto.MyPageResponse;
import com.campusnavi.backend.mypage.dto.MyScrapResponse;
import com.campusnavi.backend.official.post.dto.RecentViewResponse;
import com.campusnavi.backend.mypage.service.MyPageService;
import com.campusnavi.backend.mypage.service.MyScrapService;
import com.campusnavi.backend.official.post.dto.RecentScrapResponse;
import com.campusnavi.backend.scrap.dto.ScrapFolderResponse;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = MyPageController.class)
class MyPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyPageService myPageService;

    @MockitoBean
    private MyScrapService myScrapService;

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

    @Test
    @DisplayName("내 스크랩 화면 요청이면 200과 최근 스크랩·폴더 목록을 반환한다")
    void getMyScraps() throws Exception {
        given(myScrapService.getMyScraps(MEMBER_ID)).willReturn(new MyScrapResponse(
                List.of(new RecentScrapResponse(2L, "장학 공고", "장학", null, null)),
                List.of(new ScrapFolderResponse(100L, "취업", null, 1L))));

        mockMvc.perform(get("/api/v1/mypage/scraps").with(authentication(AUTH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recentScraps[0].title").value("장학 공고"))
                .andExpect(jsonPath("$.data.folders[0].name").value("취업"));
    }

    @Nested
    @DisplayName("최근 본 게시물 조회")
    class GetRecentViews {

        @Test
        @DisplayName("정상 요청이면 200과 커서 페이지를 반환한다")
        void success() throws Exception {
            CursorPageResponse<RecentViewResponse> page = CursorPageResponse.of(
                    List.of(new RecentViewResponse(3L, "공고 제목", "수강",
                            LocalDate.of(2026, 5, 25), LocalDateTime.of(2026, 5, 20, 12, 0))),
                    "next-cursor",
                    true);
            given(myPageService.getRecentViews(eq(MEMBER_ID), any())).willReturn(page);

            mockMvc.perform(get("/api/v1/mypage/recent-views").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].postId").value(3))
                    .andExpect(jsonPath("$.data.content[0].title").value("공고 제목"))
                    .andExpect(jsonPath("$.data.content[0].tagName").value("수강"))
                    .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
                    .andExpect(jsonPath("$.data.hasNext").value(true));
        }

        @Test
        @DisplayName("잘못된 cursor면 400과 INVALID_PARAM을 반환한다")
        void invalidCursor() throws Exception {
            willThrow(new BusinessException(ErrorCode.INVALID_PARAM))
                    .given(myPageService).getRecentViews(eq(MEMBER_ID), any());

            mockMvc.perform(get("/api/v1/mypage/recent-views")
                            .with(authentication(AUTH))
                            .param("cursor", "broken"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
        }
    }

    @Nested
    @DisplayName("최근 본 게시물 삭제")
    class DeleteRecentView {

        @Test
        @DisplayName("정상 요청이면 204를 반환한다")
        void success() throws Exception {
            willDoNothing().given(myPageService).deleteRecentView(anyLong(), anyLong());

            mockMvc.perform(delete("/api/v1/mypage/recent-views/{postId}", 5L)
                            .with(authentication(AUTH)))
                    .andExpect(status().isNoContent());

            then(myPageService).should().deleteRecentView(MEMBER_ID, 5L);
        }
    }
}
