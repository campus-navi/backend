package com.campusnavi.backend.community.comment.controller;

import com.campusnavi.backend.community.comment.dto.CommentCreateRequest;
import com.campusnavi.backend.community.comment.dto.CommentResponse;
import com.campusnavi.backend.community.comment.dto.CommentUpdateRequest;
import com.campusnavi.backend.community.comment.service.CommentService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthMember authMember) {
        return ResponseEntity.ok(ApiResponse.ok(commentService.getComments(postId, authMember)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        commentService.createComment(postId, request, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/{commentId}/replies")
    public ResponseEntity<ApiResponse<Void>> createReply(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentCreateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        commentService.createReply(postId, commentId, request, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody @Valid CommentUpdateRequest request,
            @AuthenticationPrincipal AuthMember authMember) {
        commentService.updateComment(postId, commentId, request, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthMember authMember) {
        commentService.deleteComment(postId, commentId, authMember);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
