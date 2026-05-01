package com.campusnavi.backend.official.post.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.post.dto.AttachmentResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.entity.ApplyMethodType;
import com.campusnavi.backend.official.post.service.OfficialPostService;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = OfficialPostController.class)
class OfficialPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OfficialPostService officialPostService;

    @Nested
    @DisplayName("공식 공지 상세 조회")
    class GetDetail {

        @Test
        @DisplayName("정상 요청이면 200과 상세 정보를 반환한다")
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
                    ApplyMethodType.EMAIL,
                    null,
                    "성적증명서",
                    "02-1234-5678",
                    "staff@example.com",
                    "https://cdn/img/a.png",
                    List.of(new AttachmentResponse("doc.pdf", "https://cdn/file/b.pdf"))
            );
            given(officialPostService.getDetail(1L)).willReturn(response);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postId").value(1L))
                    .andExpect(jsonPath("$.data.title").value("2026 장학금 안내"))
                    .andExpect(jsonPath("$.data.tagName").value("장학금"))
                    .andExpect(jsonPath("$.data.applyMethodType").value("EMAIL"))
                    .andExpect(jsonPath("$.data.thumbnailUrl").value("https://cdn/img/a.png"))
                    .andExpect(jsonPath("$.data.attachments[0].name").value("doc.pdf"))
                    .andExpect(jsonPath("$.data.attachments[0].url").value("https://cdn/file/b.pdf"));
        }

        @Test
        @DisplayName("존재하지 않거나 비활성화된 공지이면 404와 OFFICIAL_POST_NOT_FOUND를 반환한다")
        void notFound() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_FOUND))
                    .given(officialPostService).getDetail(99L);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("AI 후처리가 완료되지 않았으면 425와 OFFICIAL_POST_NOT_READY를 반환한다")
        void notReady() throws Exception {
            willThrow(new BusinessException(ErrorCode.OFFICIAL_POST_NOT_READY))
                    .given(officialPostService).getDetail(50L);

            mockMvc.perform(get("/api/v1/official-posts/{id}", 50L))
                    .andExpect(status().is(425))
                    .andExpect(jsonPath("$.code").value("OFFICIAL_POST_NOT_READY"));
        }
    }
}
