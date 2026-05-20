package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RecentViewResponse(
        Long postId,
        String title,
        String tagName,
        LocalDate endDate,
        LocalDateTime lastViewedAt
) {
}
