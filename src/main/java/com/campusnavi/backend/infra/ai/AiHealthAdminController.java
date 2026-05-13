package com.campusnavi.backend.infra.ai;

import com.campusnavi.backend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "6. 어드민", description = "운영 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AiHealthAdminController {

    private final AiClient aiClient;

    @Operation(summary = "FastAPI 서버 헬스체크", description = "FastAPI 서버의 /health 엔드포인트를 호출해 응답을 그대로 반환합니다.")
    @GetMapping("/ai/health")
    public ResponseEntity<ApiResponse<AiHealthResponse>> health() {
        return ResponseEntity.ok(ApiResponse.ok(aiClient.healthCheck()));
    }
}