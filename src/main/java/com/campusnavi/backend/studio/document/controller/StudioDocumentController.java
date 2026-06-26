package com.campusnavi.backend.studio.document.controller;

import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.studio.document.controller.dto.DocumentDetailResponse;
import com.campusnavi.backend.studio.document.controller.dto.DocumentSummaryResponse;
import com.campusnavi.backend.studio.document.service.StudioDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "14. [스튜디오] 내 문서함", description = "스튜디오 문서 목록·상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/studio/documents")
public class StudioDocumentController {

    private final StudioDocumentService studioDocumentService;

    @Operation(summary = "내 문서함 목록 조회",
            description = "내가 작성한 스튜디오 문서를 최근 수정순으로 조회합니다. 내 문서함 라벨용 metadata를 포함합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentSummaryResponse>>> getDocuments(
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(studioDocumentService.getDocuments(authMember.memberId())));
    }

    @Operation(summary = "문서 섹션 상세 조회",
            description = "내 문서의 섹션 content를 sortOrder 순으로 조회합니다(편집·이어쓰기용).")
    @GetMapping("/{documentId}/sections")
    public ResponseEntity<ApiResponse<DocumentDetailResponse>> getDocumentSections(
            @PathVariable Long documentId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(studioDocumentService.getDocumentSections(authMember.memberId(), documentId)));
    }
}
