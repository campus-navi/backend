package com.campusnavi.backend.official.crawler.controller;

import com.campusnavi.backend.global.exception.BusinessException;
import com.campusnavi.backend.global.exception.ErrorCode;
import com.campusnavi.backend.official.crawler.dto.CrawlStatusResponse;
import com.campusnavi.backend.official.crawler.service.CrawlerOrchestratorService;
import com.campusnavi.backend.support.ControllerSliceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ControllerSliceTest(controllers = CrawlerAdminController.class)
class CrawlerAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrawlerOrchestratorService crawlerOrchestratorService;

    @Nested
    @DisplayName("시드 크롤링 실행")
    class SeedCrawl {

        @Test
        @DisplayName("정상 요청이면 202를 반환한다")
        void success() throws Exception {
            mockMvc.perform(post("/api/v1/admin/crawl/seed"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.success").value(true));

            then(crawlerOrchestratorService).should().startSeedAsync();
        }

        @Test
        @DisplayName("실행 중이면 409를 반환한다")
        void alreadyRunning() throws Exception {
            willThrow(new BusinessException(ErrorCode.CRAWL_ALREADY_RUNNING))
                    .given(crawlerOrchestratorService).startSeedAsync();

            mockMvc.perform(post("/api/v1/admin/crawl/seed"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.CRAWL_ALREADY_RUNNING.name()));
        }
    }

    @Nested
    @DisplayName("크롤링 상태 조회")
    class CrawlStatus {

        @Test
        @DisplayName("실행 여부와 시각을 반환한다")
        void success() throws Exception {
            given(crawlerOrchestratorService.status()).willReturn(
                    new CrawlStatusResponse(true, LocalDateTime.of(2026, 7, 2, 10, 0), null));

            mockMvc.perform(get("/api/v1/admin/crawl/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.running").value(true))
                    .andExpect(jsonPath("$.data.startedAt").isNotEmpty());
        }
    }
}
