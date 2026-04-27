package com.campusnavi.backend.official.admin;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.official.ai.AiMetaAdminService;
import com.campusnavi.backend.official.ai.dto.BatchResult;
import com.campusnavi.backend.official.crawler.service.CrawlerOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. 어드민", description = "운영 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class OfficialAdminController {

    private final CrawlerOrchestratorService crawlerOrchestratorService;
    private final AiMetaAdminService aiMetaAdminService;

    @Operation(summary = "공식정보 시드 크롤링 실행", description = "전체 공식정보를 초기 수집합니다. AI 후처리 없이 DB 저장만 수행합니다.")
    @PostMapping("/crawl/seed")
    public ResponseEntity<ApiResponse<Void>> seedCrawl() {
        crawlerOrchestratorService.runSeed();
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "공식정보 AI 메타 일괄 생성", description = "PENDING 상태인 게시글을 최대 limit건 FastAPI로 배치 처리합니다. 시드 크롤링 후 초기 데이터 세팅 시 사용합니다.")
    @PostMapping("/ai-meta/process-pending")
    public ResponseEntity<ApiResponse<String>> processPending(@RequestParam(defaultValue = "100") int limit) {
        BatchResult result = aiMetaAdminService.processPendingBatch(limit);
        return ResponseEntity.ok(
                ApiResponse.ok("성공: " + result.success() + ", 실패: " + result.failure()));
    }
}
