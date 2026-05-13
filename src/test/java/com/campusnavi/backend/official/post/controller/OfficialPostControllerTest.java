package com.campusnavi.backend.official.post.controller;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.official.post.dto.AttachmentDownloadResponse;
import com.campusnavi.backend.official.post.dto.AttachmentResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostSummaryResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostListSort;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.service.OfficialAttachmentDownloadService;
import com.campusnavi.backend.official.post.service.OfficialPostNotificationService;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.official.post.service.OfficialPostService;
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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = OfficialPostController.class)
class OfficialPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfficialPostService officialPostService;

    @MockitoBean
    private OfficialPostScrapService officialPostScrapService;

    @MockitoBean
    private OfficialPostNotificationService officialPostNotificationService;

    @MockitoBean
    private OfficialAttachmentDownloadService officialAttachmentDownloadService;

    private static final Long MEMBER_ID = 1L;
    private static final Long UNIVERSITY_ID = 10L;
    private static final AuthContext CONTEXT = new AuthContext(MEMBER_ID, UNIVERSITY_ID);
    private static final Authentication AUTH = new UsernamePasswordAuthenticationToken(
            new AuthMember(MEMBER_ID, "USER", UNIVERSITY_ID), null, List.of()
    );

    @Nested
    @DisplayName("공식 정보 목록 조회")
    class GetList {

        private static final OfficialPostSummaryResponse ITEM =
                new OfficialPostSummaryResponse(1L, "장학금 안내", "장학", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31));

        @Test
        @DisplayName("파라미터 없이 호출하면 LATEST/null/null/null로 서비스에 위임하고 200을 반환한다")
        void defaultParams() throws Exception {
            CursorPageResponse<OfficialPostSummaryResponse> response =
                    CursorPageResponse.of(List.of(ITEM), null, false);
            given(officialPostService.getList(eq(CONTEXT), isNull(), isNull(),
                    eq(OfficialPostListSort.LATEST), isNull())).willReturn(response);

            mockMvc.perform(get("/api/v1/official-posts").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].postId").value(1L))
                    .andExpect(jsonPath("$.data.content[0].title").value("장학금 안내"))
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        }

        @Test
        @DisplayName("q, tagCode, sort, cursor를 전달하면 각 인자가 서비스에 그대로 위임된다")
        void withAllParams() throws Exception {
            given(officialPostService.getList(eq(CONTEXT), eq("장학"), eq("SCHOLARSHIP"),
                    eq(OfficialPostListSort.DEADLINE), eq("abc123")))
                    .willReturn(CursorPageResponse.of(List.of(), null, false));

            mockMvc.perform(get("/api/v1/official-posts")
                            .param("q", "장학")
                            .param("tagCode", "SCHOLARSHIP")
                            .param("sort", "DEADLINE")
                            .param("cursor", "abc123")
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("hasNext=true이면 nextCursor가 응답에 포함된다")
        void withNextCursor() throws Exception {
            CursorPageResponse<OfficialPostSummaryResponse> response =
                    CursorPageResponse.of(List.of(ITEM), "nextToken", true);
            given(officialPostService.getList(any(), any(), any(), any(), any()))
                    .willReturn(response);

            mockMvc.perform(get("/api/v1/official-posts").with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasNext").value(true))
                    .andExpect(jsonPath("$.data.nextCursor").value("nextToken"));
        }

        @Test
        @DisplayName("잘못된 sort 값이면 400을 반환한다")
        void invalidSort() throws Exception {
            mockMvc.perform(get("/api/v1/official-posts")
                            .param("sort", "WRONG")
                            .with(authentication(AUTH)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("잘못된 cursor로 서비스가 INVALID_PARAM을 던지면 400을 반환한다")
        void invalidCursor() throws Exception {
            given(officialPostService.getList(any(), any(), any(), any(), eq("bad")))
                    .willThrow(new BusinessException(ErrorCode.INVALID_PARAM));

            mockMvc.perform(get("/api/v1/official-posts")
                            .param("cursor", "bad")
                            .with(authentication(AUTH)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PARAM"));
        }
    }

    @Nested
    @DisplayName("공식 공지 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 요청이면 200과 상세 정보를 반환하며 isScrapped/hasUnreadAttachments/attachments 새 스키마를 포함한다")
        void success() throws Exception {
            OfficialPostDetailResponse response = new OfficialPostDetailResponse(
                    1L,
                    "2026 장학금 안내",
                    "학사팀",
                    "https://example.com/notice/1",
                    LocalDate.of(2026, 4, 1),
                    "장학금",
                    "장학금 신청 안내입니다.",
                    "<p>본문</p>",
                    true,
                    LocalDate.of(2026, 4, 1),
                    LocalTime.of(9, 0),
                    LocalDate.of(2026, 5, 31),
                    null,
                    "재학생",
                    ApplyMethodType.FILE,
                    null,
                    "성적증명서",
                    "02-1234-5678",
                    "staff@example.com",
                    List.of("https://cdn/img/a.png"),
                    List.of(new AttachmentResponse(910L, "doc.pdf", false)),
                    true,
                    true,
                    false
            );
            given(officialPostService.getDetail(1L, CONTEXT)).willReturn(response);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 1L).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postId").value(1L))
                    .andExpect(jsonPath("$.data.title").value("2026 장학금 안내"))
                    .andExpect(jsonPath("$.data.tagName").value("장학금"))
                    .andExpect(jsonPath("$.data.applyMethodType").value("FILE"))
                    .andExpect(jsonPath("$.data.imageUrls.length()").value(1))
                    .andExpect(jsonPath("$.data.imageUrls[0]").value("https://cdn/img/a.png"))
                    .andExpect(jsonPath("$.data.attachments[0].id").value(910L))
                    .andExpect(jsonPath("$.data.attachments[0].name").value("doc.pdf"))
                    .andExpect(jsonPath("$.data.attachments[0].isDownloaded").value(false))
                    .andExpect(jsonPath("$.data.hasUnreadAttachments").value(true))
                    .andExpect(jsonPath("$.data.isScrapped").value(true))
                    .andExpect(jsonPath("$.data.isNotificationOn").value(false));
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화된 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostService).getDetail(99L, CONTEXT);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 99L).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("AI 후처리가 완료되지 않았으면 425와 OFFICIAL_POST_NOT_READY를 반환한다")
        void notReady() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_READY))
                    .given(officialPostService).getDetail(50L, CONTEXT);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 50L).with(authentication(AUTH)))
                    .andExpect(status().is(425))
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_READY"));
        }
    }

    @Nested
    @DisplayName("스크랩 추가")
    class Scrap {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(officialPostScrapService).scrap(1L, CONTEXT);

            mockMvc.perform(put("/api/v1/official-posts/{id}/scrap", 1L).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(officialPostScrapService).should().scrap(1L, CONTEXT);
        }

        @Test
        @DisplayName("존재하지 않는 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostScrapService).scrap(99L, CONTEXT);

            mockMvc.perform(put("/api/v1/official-posts/{id}/scrap", 99L).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("스크랩 해제")
    class Unscrap {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(officialPostScrapService).unscrap(1L, CONTEXT);

            mockMvc.perform(delete("/api/v1/official-posts/{id}/scrap", 1L).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(officialPostScrapService).should().unscrap(1L, CONTEXT);
        }

        @Test
        @DisplayName("존재하지 않는 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostScrapService).unscrap(99L, CONTEXT);

            mockMvc.perform(delete("/api/v1/official-posts/{id}/scrap", 99L).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("알림 켜기")
    class EnableNotification {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(officialPostNotificationService).enable(1L, CONTEXT);

            mockMvc.perform(put("/api/v1/official-posts/{id}/notification", 1L).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(officialPostNotificationService).should().enable(1L, CONTEXT);
        }

        @Test
        @DisplayName("존재하지 않는 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostNotificationService).enable(99L, CONTEXT);

            mockMvc.perform(put("/api/v1/official-posts/{id}/notification", 99L).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("알림 끄기")
    class DisableNotification {

        @Test
        @DisplayName("정상 요청이면 200을 반환한다")
        void success() throws Exception {
            willDoNothing().given(officialPostNotificationService).disable(1L, CONTEXT);

            mockMvc.perform(delete("/api/v1/official-posts/{id}/notification", 1L).with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            then(officialPostNotificationService).should().disable(1L, CONTEXT);
        }

        @Test
        @DisplayName("존재하지 않는 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostNotificationService).disable(99L, CONTEXT);

            mockMvc.perform(delete("/api/v1/official-posts/{id}/notification", 99L).with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("첨부파일 다운로드 URL 발급")
    class DownloadAttachment {

        @Test
        @DisplayName("정상 요청이면 200과 downloadUrl/expiresInSeconds를 반환한다")
        void success() throws Exception {
            given(officialAttachmentDownloadService.issueDownloadUrl(1L, 910L, CONTEXT))
                    .willReturn(new AttachmentDownloadResponse("https://signed.example/doc.pdf", 600L));

            mockMvc.perform(get("/api/v1/official-posts/{postId}/attachments/{attachmentId}/download", 1L, 910L)
                            .with(authentication(AUTH)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.downloadUrl").value("https://signed.example/doc.pdf"))
                    .andExpect(jsonPath("$.data.expiresInSeconds").value(600));
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화/스코프 밖 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void postNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialAttachmentDownloadService).issueDownloadUrl(99L, 910L, CONTEXT);

            mockMvc.perform(get("/api/v1/official-posts/{postId}/attachments/{attachmentId}/download", 99L, 910L)
                            .with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("존재하지 않거나 post와 일치하지 않는 첨부이면 404와 OFFICIAL_ATTACHMENT_NOT_FOUND를 반환한다")
        void attachmentNotFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_ATTACHMENT_NOT_FOUND))
                    .given(officialAttachmentDownloadService).issueDownloadUrl(1L, 999L, CONTEXT);

            mockMvc.perform(get("/api/v1/official-posts/{postId}/attachments/{attachmentId}/download", 1L, 999L)
                            .with(authentication(AUTH)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_ATTACHMENT_NOT_FOUND"));
        }
    }
}
