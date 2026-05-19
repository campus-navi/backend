package com.campusnavi.backend.notification.controller;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.notification.dto.MissedNotice;
import com.campusnavi.backend.notification.dto.MissedNoticeCard;
import com.campusnavi.backend.notification.dto.RemindNotice;
import com.campusnavi.backend.notification.service.ActivityNotificationService;
import com.campusnavi.backend.notification.service.RemindNotificationService;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityNotificationService activityNotificationService;

    @MockitoBean
    private RemindNotificationService remindNotificationService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID), null, List.of()
    );

    @Nested
    @DisplayName("지나친 공지 카드 목록 조회")
    class GetCards {

        @Test
        @DisplayName("정상 요청이면 200과 카드 배열을 반환한다")
        void success() throws Exception {
            given(activityNotificationService.getActivityCards(CONTEXT)).willReturn(List.of(
                    new MissedNoticeCard(LocalDate.of(2026, 5, 14), 3),
                    new MissedNoticeCard(LocalDate.of(2026, 5, 13), 2)
            ));

            mockMvc.perform(get("/api/v1/notifications/activity").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].missedDate").value("2026-05-14"))
                    .andExpect(jsonPath("$.data[0].count").value(3))
                    .andExpect(jsonPath("$.data[1].missedDate").value("2026-05-13"))
                    .andExpect(jsonPath("$.data[1].count").value(2));
        }

        @Test
        @DisplayName("결과가 비어 있으면 200과 빈 배열을 반환한다")
        void empty() throws Exception {
            given(activityNotificationService.getActivityCards(CONTEXT)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/notifications/activity").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }

    @Nested
    @DisplayName("지나친 공지 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 요청이면 200과 미열람 공지 목록을 반환한다")
        void success() throws Exception {
            LocalDate missedDate = LocalDate.of(2026, 5, 13);
            given(activityNotificationService.getActivityDetail(CONTEXT, missedDate))
                    .willReturn(List.of(
                            new MissedNotice(11L, "장학금 안내", "장학",
                                    LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                            new MissedNotice(12L, "축제 공지", "행사",
                                    LocalDate.of(2026, 5, 2), null)));

            mockMvc.perform(get("/api/v1/notifications/activity/{missedDate}", "2026-05-13")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].postId").value(11L))
                    .andExpect(jsonPath("$.data[0].title").value("장학금 안내"))
                    .andExpect(jsonPath("$.data[0].tagName").value("장학"))
                    .andExpect(jsonPath("$.data[0].publishedAt").value("2026-05-01"))
                    .andExpect(jsonPath("$.data[0].endDate").value("2026-05-31"))
                    .andExpect(jsonPath("$.data[1].postId").value(12L))
                    .andExpect(jsonPath("$.data[1].endDate").doesNotExist());
        }

        @Test
        @DisplayName("스냅샷이 없으면 404와 ACTIVITY_NOTIFICATION_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            LocalDate missedDate = LocalDate.of(2025, 1, 1);
            willThrow(new BusinessException(ErrorCode.ACTIVITY_NOTIFICATION_NOT_FOUND))
                    .given(activityNotificationService).getActivityDetail(CONTEXT, missedDate);

            mockMvc.perform(get("/api/v1/notifications/activity/{missedDate}", "2025-01-01")
                            .with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ACTIVITY_NOTIFICATION_NOT_FOUND"));
        }

        @Test
        @DisplayName("잘못된 날짜 포맷이면 400을 반환한다")
        void invalidDate() throws Exception {
            mockMvc.perform(get("/api/v1/notifications/activity/{missedDate}", "not-a-date")
                            .with(authentication(AUTH)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("리마인드 공지 목록 조회")
    class GetRemind {

        @Test
        @DisplayName("정상 요청이면 200과 마감 임박 순 목록을 반환한다")
        void success() throws Exception {
            given(remindNotificationService.getRemindNotices(CONTEXT)).willReturn(List.of(
                    new RemindNotice(11L, "장학금 신청", "장학", LocalDate.of(2026, 5, 16)),
                    new RemindNotice(12L, "동아리 모집", "행사", LocalDate.of(2026, 5, 20))));

            mockMvc.perform(get("/api/v1/notifications/remind").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].postId").value(11L))
                    .andExpect(jsonPath("$.data[0].title").value("장학금 신청"))
                    .andExpect(jsonPath("$.data[0].tagName").value("장학"))
                    .andExpect(jsonPath("$.data[0].endDate").value("2026-05-16"))
                    .andExpect(jsonPath("$.data[1].postId").value(12L));
        }

        @Test
        @DisplayName("결과가 비어 있으면 200과 빈 배열을 반환한다")
        void empty() throws Exception {
            given(remindNotificationService.getRemindNotices(CONTEXT)).willReturn(List.of());

            mockMvc.perform(get("/api/v1/notifications/remind").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }
}
