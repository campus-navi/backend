package com.campusnavi.backend.studio.academicplan.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.academicplan.dto.DocumentCreateRequest;
import com.campusnavi.backend.studio.academicplan.service.AcademicPlanDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "12. [스튜디오] 학업계획서", description = "학업계획서 작성 대상(캠퍼스/학과/전공) 선택 및 문서 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic-plans/documents")
public class AcademicPlanDocumentController {

    private final AcademicPlanDocumentService academicPlanDocumentService;

    @Operation(summary = "학업계획서 생성",
            description = "지원대상(majorType + targetId)과 작성된 섹션으로 학업계획서를 임시저장(DRAFT)합니다. 섹션은 1개 이상이어야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "섹션 검증 실패(누락·길이 초과·잘못된 sectionKey) 또는 지원 불가 대상",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상(targetId)을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> create(
            @RequestBody @Valid DocumentCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        academicPlanDocumentService.create(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }
}
