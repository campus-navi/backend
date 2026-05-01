package com.campusnavi.backend.official.post.controller;

import com.campusnavi.backend.global.common.AuthContext;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.official.post.dto.AttachmentDownloadResponse;
import com.campusnavi.backend.official.post.dto.OfficialPostDetailResponse;
import com.campusnavi.backend.official.post.service.OfficialAttachmentDownloadService;
import com.campusnavi.backend.official.post.service.OfficialPostScrapService;
import com.campusnavi.backend.official.post.service.OfficialPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "9. 공식 정보", description = "공식 정보 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/official-posts")
public class OfficialPostController {

    private final OfficialPostService officialPostService;
    private final OfficialPostScrapService officialPostScrapService;
    private final OfficialAttachmentDownloadService officialAttachmentDownloadService;

    @Operation(summary = "공식 정보 상세 조회", description = "공식 정보의 상세 내용(본문, AI 메타, 첨부파일 포함)을 반환합니다. 인증된 사용자의 스크랩 여부도 함께 반환됩니다. 사용자의 university scope 밖 공지에는 접근할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성화/스코프 밖 공지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "425", description = "AI 후처리 미완료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfficialPostDetailResponse>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                officialPostService.getDetail(id, AuthContext.of(authMember))));
    }

    @Operation(summary = "공식 정보 스크랩 추가", description = "공식 정보를 스크랩합니다. 이미 스크랩된 상태이면 멱등 동작합니다. 사용자의 university scope 밖 공지는 스크랩할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스크랩 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성화/스코프 밖 공지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}/scrap")
    public ResponseEntity<ApiResponse<Void>> scrap(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthMember authMember) {
        officialPostScrapService.scrap(id, AuthContext.of(authMember));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "공식 정보 스크랩 해제", description = "공식 정보 스크랩을 해제합니다. 스크랩되지 않은 상태이면 멱등 동작합니다. 사용자의 university scope 밖 공지는 해제할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스크랩 해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성화/스코프 밖 공지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}/scrap")
    public ResponseEntity<ApiResponse<Void>> unscrap(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthMember authMember) {
        officialPostScrapService.unscrap(id, AuthContext.of(authMember));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "공식 정보 첨부파일 다운로드 URL 발급",
            description = "공식 정보 첨부파일에 대한 단기 유효 presigned URL을 발급하고 다운로드 이력을 적재합니다. " +
                    "응답의 downloadUrl로 클라이언트가 직접 다운로드 합니다 (Content-Disposition: attachment 강제). " +
                    "사용자의 university scope 밖 공지의 첨부는 다운로드할 수 없습니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "다운로드 URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않거나 비활성화/스코프 밖 공지 또는 첨부파일",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{postId}/attachments/{attachmentId}/download")
    public ResponseEntity<ApiResponse<AttachmentDownloadResponse>> downloadAttachment(
            @PathVariable Long postId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(
                officialAttachmentDownloadService.issueDownloadUrl(postId, attachmentId, AuthContext.of(authMember))));
    }
}
