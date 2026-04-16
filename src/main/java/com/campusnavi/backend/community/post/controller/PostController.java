package com.campusnavi.backend.community.post.controller;

import com.campusnavi.backend.community.post.dto.PostCreateRequest;
import com.campusnavi.backend.community.post.dto.PostCreateResponse;
import com.campusnavi.backend.community.post.service.PostService;
import com.campusnavi.backend.global.response.ApiResponse;
import com.campusnavi.backend.infra.storage.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<ApiResponse<PostCreateResponse>> createPost(
            @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(ApiResponse.ok(postService.createPost(memberId, request)));
    }
}
