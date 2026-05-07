package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record DeadlinePostRaw(
        Long postId,
        String title,
        String tagName,
        String publisher,
        LocalDate publishedAt,
        LocalDate endDate,
        boolean isNotificationOn
) {
}