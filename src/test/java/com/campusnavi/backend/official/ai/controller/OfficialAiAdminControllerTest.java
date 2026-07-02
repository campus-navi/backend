package com.campusnavi.backend.official.ai.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.ai.dto.AiMetaStatusResponse;
import com.campusnavi.backend.official.ai.service.AiMetaAdminService;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = OfficialAiAdminController.class)
class OfficialAiAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiMetaAdminService aiMetaAdminService;

    @Nested
    @DisplayName("AI 메타 일괄 생성")
    class ProcessPending {

        @Test
        @DisplayName("정상 요청이면 202를 반환한다")
        void success() throws Exception {
            mockMvc.perform(post("/api/v1/admin/ai-meta/process-pending")
                            .param("limit", "50")
                            .param("campusId", "1"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));

            then(aiMetaAdminService).should().startProcessPendingAsync(50, 1L, null);
        }

        @Test
        @DisplayName("실행 중이면 409를 반환한다")
        void alreadyRunning() throws Exception {
            willThrow(new BusinessException(ErrorCode.AI_BATCH_ALREADY_RUNNING))
                    .given(aiMetaAdminService).startProcessPendingAsync(anyInt(), any(), any());

            mockMvc.perform(post("/api/v1/admin/ai-meta/process-pending"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.AI_BATCH_ALREADY_RUNNING.name()));
        }
    }

    @Nested
    @DisplayName("AI 메타 상태 조회")
    class AiMetaStatus {

        @Test
        @DisplayName("실행 여부와 카운트를 반환한다")
        void success() throws Exception {
            given(aiMetaAdminService.status()).willReturn(
                    new AiMetaStatusResponse(true, LocalDateTime.of(2026, 7, 2, 10, 0), null, 5L, 2L, 1L));

            mockMvc.perform(get("/api/v1/admin/ai-meta/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.running").value(true))
                    .andExpect(jsonPath("$.data.pendingCount").value(5))
                    .andExpect(jsonPath("$.data.failedCount").value(1));
        }
    }
}
