package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record DeadlinePostResponse(
        Long postId,
        String title,
        String tagName,
        String publisher,
        LocalDate publishedAt,
        LocalDate endDate,
        LocalTime endTime,
        boolean isNotificationOn
) {
}
