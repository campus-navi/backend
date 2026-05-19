package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record RecentScrapResponse(
        Long postId,
        String title,
        String tagName,
        LocalDate endDate,
        LocalDate publishedAt
) {
}
