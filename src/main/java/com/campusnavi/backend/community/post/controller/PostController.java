package com.campusnavi.backend.community.post.controller;

import com.campusnavi.backend.community.post.dto.*;
import com.campusnavi.backend.community.post.service.PostInteractionService;
import com.campusnavi.backend.community.post.service.PostService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.response.CursorPageResponse;
import com.campusnavi.backend.global.security.AuthMember;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;
    private final PostInteractionService postInteractionService;

    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<PostSummaryResponse>>> getPosts(
            @RequestParam(defaultValue = "LATEST") ViewType viewType,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPosts(authMember, viewType, cursor, size)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestBody @Valid PostCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(postService.createPost(authMember, request)));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(postService.getPost(postId, authMember)));
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @RequestBody @Valid PostPresignedUrlRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(postService.generatePostPresignedUrl(request)));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid PostUpdateRequest request) {
        postService.updatePost(postId, authMember, request);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postService.deletePost(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Void>> addLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.addLike(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{postId}/likes")
    public ResponseEntity<ApiResponse<Void>> removeLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.removeLike(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PutMapping("/{postId}/scraps")
    public ResponseEntity<ApiResponse<Void>> addScrap(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.addScrap(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{postId}/scraps")
    public ResponseEntity<ApiResponse<Void>> removeScrap(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        postInteractionService.removeScrap(postId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
