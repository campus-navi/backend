package com.campusnavi.backend.community.post.controller;

import com.campusnavi.backend.community.post.dto.*;
import com.campusnavi.backend.community.post.service.PostInteractionService;
import com.campusnavi.backend.community.post.service.PostService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.global.response.ErrorResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3. 게시글", description = "게시글 CRUD, 좋아요/스크랩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final PostInteractionService postInteractionService;

    @Operation(summary = "게시글 목록 조회", description = "커서 기반 페이징으로 게시글 목록을 조회합니다. viewType: LATEST(최신순), POPULAR(인기순), SCRAP(스크랩순)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PostSummaryResponse>>> getPosts(
            @Parameter(description = "조회 유형 (LATEST | POPULAR | SCRAP)", example = "LATEST")
            @RequestParam(defaultValue = "LATEST") ViewType view,
            @Parameter(description = "다음 페이지 커서 (첫 요청 시 생략)")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (기본값 20)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPosts(authMember, view, cursor, size)));
    }

    @Operation(summary = "게시글 작성")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(postService.createPost(authMember, request)));
    }

    @Operation(summary = "게시글 상세 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(postId, authMember)));
    }

    @Operation(summary = "이미지 업로드용 Presigned URL 발급", description = "S3 직접 업로드를 위한 Presigned URL과 imageKey를 반환합니다. 게시글 작성/수정 시 imageKeys 필드에 key 사용하시면 됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @RequestBody @Valid PostPresignedUrlRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.generatePostPresignedUrl(request)));
    }

    @Operation(summary = "게시글 수정")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아닌 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid PostUpdateRequest request) {
        postService.updatePost(postId, authMember, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "게시글 삭제")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아닌 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postService.deletePost(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "게시글 좋아요 추가")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 좋아요한 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Void>> addLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.addLike(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "게시글 좋아요 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.removeLike(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "게시글 스크랩 추가")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스크랩 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 스크랩한 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{postId}/scraps")
    public ResponseEntity<ApiResponse<Void>> addScrap(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.addScrap(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "게시글 스크랩 취소")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "스크랩 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 게시글",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{postId}/scraps")
    public ResponseEntity<ApiResponse<Void>> removeScrap(
            @Parameter(description = "게시글 ID") @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.removeScrap(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
