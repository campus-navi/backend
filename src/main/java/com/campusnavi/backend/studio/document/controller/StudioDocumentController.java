package com.campusnavi.backend.studio.document.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.document.controller.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentUpdateRequest;
import com.campusnavi.backend.studio.document.service.StudioDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "13. [스튜디오] 내 문서함", description = "스튜디오 문서 목록·상세 조회, 문서 수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/studio/documents")
public class StudioDocumentController {

    private final StudioDocumentService studioDocumentService;

    @Operation(summary = "내 문서함 목록 조회",
            description = "내가 작성한 스튜디오 문서를 최근 수정순으로 조회합니다. 내 문서함 라벨용 metadata를 포함합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentSummaryResponse>>> getDocuments(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(studioDocumentService.getDocuments(authMember.memberId())));
    }

    @Operation(summary = "문서 섹션 상세 조회",
            description = "내 문서의 섹션 content를 sortOrder 순으로 조회합니다(편집·이어쓰기용).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 타인 소유 문서",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{documentId}/sections")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocumentSections(
            @PathVariable Long documentId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(studioDocumentService.getDocumentSections(authMember.memberId(), documentId)));
    }

    @Operation(summary = "문서 섹션 수정(이어쓰기)",
            description = "내 문서의 섹션을 upsert합니다(미작성이면 추가, 작성돼 있으면 수정). metadata는 변경되지 않습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "섹션 검증 실패(길이 초과·잘못된 sectionKey)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 타인 소유 문서",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{documentId}")
    public ResponseEntity<ApiResponse<Void>> updateSections(
            @PathVariable Long documentId,
            @RequestBody @Valid DocumentUpdateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        studioDocumentService.updateSections(authMember.memberId(), documentId, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
