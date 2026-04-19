package com.campusnavi.backend.community.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Long id,
        String nickname,
        String title,
        String content,
        LocalDateTime createdAt,
        int likeCount,
        int commentCount,
        int scrapCount,
        List<String> imageUrls,
        boolean isAnonymous,
        boolean isLiked,
        boolean isScraped,
        boolean isMine
) {
}
