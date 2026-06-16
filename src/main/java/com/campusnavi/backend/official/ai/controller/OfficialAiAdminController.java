package com.campusnavi.backend.official.ai.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.official.ai.dto.BatchResult;
import com.campusnavi.backend.official.ai.service.AiMetaAdminService;
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
public class OfficialAiAdminController {

    private final AiMetaAdminService aiMetaAdminService;

    @Operation(summary = "공식정보 AI 메타 일괄 생성", description = "PENDING 상태인 게시글을 최신순으로 최대 limit건 FastAPI로 배치 처리합니다. campusId만 전달하면 학과 없는 해당 캠퍼스 공지, departmentId를 전달하면 해당 학과 공지를 처리합니다.")
    @PostMapping("/ai-meta/process-pending")
    public ResponseEntity<ApiResponse<String>> processPending(@RequestParam(defaultValue = "100") int limit,
                                                              @RequestParam(required = false) Long campusId,
                                                              @RequestParam(required = false) Long departmentId) {
        BatchResult result = aiMetaAdminService.processPendingBatch(limit, campusId, departmentId);
        return ResponseEntity.ok(
                ApiResponse.ok("성공: " + result.success() + ", 실패: " + result.failure()));
    }
}
