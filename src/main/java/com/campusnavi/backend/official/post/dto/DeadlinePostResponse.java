package com.campusnavi.backend.official.post.dto;

import java.time.LocalDate;

public record DeadlinePostResponse(
        Long postId,
        String title,
        String tagName,
        String publisher,
        LocalDate publishedAt,
        LocalDate endDate
) {
}
