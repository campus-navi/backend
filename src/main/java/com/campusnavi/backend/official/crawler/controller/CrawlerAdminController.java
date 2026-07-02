package com.campusnavi.backend.official.crawler.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.official.crawler.dto.CrawlStatusResponse;
import com.campusnavi.backend.official.crawler.service.CrawlerOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. 어드민", description = "운영 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class CrawlerAdminController {

    private final CrawlerOrchestratorService crawlerOrchestratorService;

    @Operation(summary = "공식정보 시드 크롤링 실행", description = "전체 공식정보 초기 수집을 비동기로 시작합니다. AI 후처리 없이 DB 저장만 수행하며, 진행 상태는 크롤링 상태 조회 API로 확인합니다.")
    @PostMapping("/crawl/seed")
    public ResponseEntity<ApiResponse<Void>> seedCrawl() {
        crawlerOrchestratorService.startSeedAsync();
        return ResponseEntity.accepted().body(ApiResponse.ok());
    }

    @Operation(summary = "크롤링 상태 조회", description = "크롤링 실행 여부와 마지막 실행 시작/종료 시각을 반환합니다.")
    @GetMapping("/crawl/status")
    public ResponseEntity<ApiResponse<CrawlStatusResponse>> crawlStatus() {
        return ResponseEntity.ok(ApiResponse.ok(crawlerOrchestratorService.status()));
    }
}
