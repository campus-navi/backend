package com.campusnavi.backend.official.post.dto;

public record OfficialPostDetailResponse(
        Long postId,
        String title,
        String publisher
) {
}
