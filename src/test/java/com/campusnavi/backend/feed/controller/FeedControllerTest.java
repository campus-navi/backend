package com.campusnavi.backend.feed.controller;

import com.campusnavi.backend.feed.dto.CardListResponse;
import com.campusnavi.backend.feed.dto.DeadlineListResponse;
import com.campusnavi.backend.feed.service.FeedService;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.official.post.dto.DeadlinePostResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostCardResponse;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = FeedController.class)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(1L, "USER", 10L), null, List.of()
    );

    @Nested
    @DisplayName("피드 카드 조회")
    class GetFeedCards {

        @Test
        @DisplayName("정상 요청이면 200과 최신/추천 카드 목록을 반환한다")
        void success() throws Exception {
            OfficialPostCardResponse card = new OfficialPostCardResponse(
                    1L, "2026 장학금 안내", "장학금", "장학금 신청 안내입니다.", null, LocalDate.of(2026, 4, 1));
            given(feedService.getCardLists(any())).willReturn(new CardListResponse(List.of(card), List.of()));

            mockMvc.perform(get("/api/v1/feed/cards").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.newPosts[0].postId").value(1L))
                    .andExpect(jsonPath("$.data.newPosts[0].title").value("2026 장학금 안내"))
                    .andExpect(jsonPath("$.data.recommendedPosts").isEmpty());
        }

        @Test
        @DisplayName("최신/추천 모두 빈 목록이어도 200을 반환한다")
        void empty() throws Exception {
            given(feedService.getCardLists(any())).willReturn(new CardListResponse(List.of(), List.of()));

            mockMvc.perform(get("/api/v1/feed/cards").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newPosts").isEmpty())
                    .andExpect(jsonPath("$.data.recommendedPosts").isEmpty());
        }
    }

    @Nested
    @DisplayName("마감임박 미리보기 조회")
    class GetDeadlinePreview {

        @Test
        @DisplayName("정상 요청이면 200과 마감임박 미리보기 목록을 반환한다")
        void success() throws Exception {
            DeadlinePostResponse post = new DeadlinePostResponse(
                    1L, "프로젝트 신청 안내", "수강", "학사팀", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 7), false);
            given(feedService.getDeadlinePostsForFeed(any())).willReturn(new DeadlineListResponse(List.of(post)));

            mockMvc.perform(get("/api/v1/feed/deadlines/preview").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.posts[0].postId").value(1L))
                    .andExpect(jsonPath("$.data.posts[0].title").value("프로젝트 신청 안내"))
                    .andExpect(jsonPath("$.data.posts[0].endDate").value("2026-04-07"))
                    .andExpect(jsonPath("$.data.posts[0].isNotificationOn").value(false));
        }

        @Test
        @DisplayName("마감임박 공지가 없으면 200과 빈 목록을 반환한다")
        void empty() throws Exception {
            given(feedService.getDeadlinePostsForFeed(any())).willReturn(new DeadlineListResponse(List.of()));

            mockMvc.perform(get("/api/v1/feed/deadlines/preview").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isEmpty());
        }
    }

    @Nested
    @DisplayName("마감임박 전체 조회")
    class GetAllDeadlines {

        @Test
        @DisplayName("정상 요청이면 200과 전체 마감임박 목록을 반환한다")
        void success() throws Exception {
            DeadlinePostResponse post = new DeadlinePostResponse(
                    2L, "장학금 신청", "장학금", "장학팀", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 5), false);
            given(feedService.getAllDeadlinePosts(any())).willReturn(new DeadlineListResponse(List.of(post)));

            mockMvc.perform(get("/api/v1/feed/deadlines").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.posts[0].postId").value(2L))
                    .andExpect(jsonPath("$.data.posts[0].endDate").value("2026-04-05"));
        }

        @Test
        @DisplayName("마감임박 공지가 없으면 200과 빈 목록을 반환한다")
        void empty() throws Exception {
            given(feedService.getAllDeadlinePosts(any())).willReturn(new DeadlineListResponse(List.of()));

            mockMvc.perform(get("/api/v1/feed/deadlines").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.posts").isEmpty());
        }
    }
}
