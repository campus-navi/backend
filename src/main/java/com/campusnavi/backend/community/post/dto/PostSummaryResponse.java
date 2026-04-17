package com.campusnavi.backend.community.post.dto;

import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long postId,
        String nickname,
        String title,
        String contentPreview,
        int likeCount,
        int scrapCount,
        int commentCount,
        LocalDateTime createdAt,
        boolean isLiked,
        boolean isScraped
) {
}
